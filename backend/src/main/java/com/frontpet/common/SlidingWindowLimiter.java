package com.frontpet.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de eventos por clave opaca en una ventana deslizante, en memoria.
 * Núcleo compartido por los 3 rate limits del sistema (login, orders,
 * appointments — ver {@code docs/pending-decisions.md} §9): la mecánica era
 * copia casi literal en los 3 módulos, solo divergían en qué cuenta como
 * "evento" (fallo vs toda request) y en si hay reset por éxito — eso queda
 * en cada `*RateLimitService` que envuelve esta clase, no acá.
 *
 * <p>En memoria y no en DB: una sola instancia en un solo VPS (ADR 016), y un
 * write a Postgres por evento sería amplificar en la DB un tráfico que el
 * atacante controla. Un reinicio borra los contadores — bypass real pero
 * acotado (ver ADR 019).
 *
 * <p>No es un {@code @Service}: cada módulo instancia su propia copia (con su
 * propio {@code Clock} y config), porque cada rate limit necesita su propio
 * mapa aislado — no tiene sentido compartir un único bean con estado entre
 * login/orders/appointments.
 */
public class SlidingWindowLimiter {

    /**
     * Tope duro de claves rastreadas. Un mapa sin límite indexado por IP es,
     * él mismo, un vector de DoS por memoria. 10k × ~100 bytes ≈ 1 MB.
     */
    public static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final boolean enabled;
    private final int maxAttempts;
    private final Duration window;

    public SlidingWindowLimiter(Clock clock, boolean enabled, int maxAttempts, Duration window) {
        this.clock = clock;
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    private record Attempt(int count, Instant windowStartedAt, Instant blockedUntil) {

        boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        boolean windowExpired(Instant now, Duration window) {
            return now.isAfter(windowStartedAt.plus(window));
        }
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        if (!enabled) {
            return 0;
        }
        Attempt attempt = attempts.get(key);
        Instant now = clock.instant();
        if (attempt == null || !attempt.isBlocked(now)) {
            return 0;
        }
        return Duration.between(now, attempt.blockedUntil()).toSeconds() + 1;
    }

    /**
     * Registra un evento para {@code key}. Al llegar a {@code maxAttempts}
     * dentro de la ventana, bloquea la clave hasta que la ventana vuelva a
     * vencer. Qué cuenta como "evento" (todo intento, o solo los fallidos) lo
     * decide quien llama.
     */
    public void recordAttempt(String key) {
        if (!enabled) {
            return;
        }
        sweepIfNeeded();
        Instant now = clock.instant();
        attempts.compute(key, (k, prev) -> {
            if (prev == null || prev.windowExpired(now, window)) {
                return new Attempt(1, now, null);
            }
            int count = prev.count() + 1;
            Instant blockedUntil = count >= maxAttempts ? now.plus(window) : null;
            return new Attempt(count, prev.windowStartedAt(), blockedUntil);
        });
    }

    /** Limpia el historial de una clave (p. ej. tras un login exitoso). */
    public void reset(String key) {
        attempts.remove(key);
    }

    /** Solo para tests: el bean que envuelve esto suele ser singleton entre clases de test. */
    public void clear() {
        attempts.clear();
    }

    /** Solo para tests: verificar que el barrido perezoso acota el mapa. */
    public int trackedKeyCount() {
        return attempts.size();
    }

    private void sweepIfNeeded() {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        Instant now = clock.instant();
        attempts.values().removeIf(a -> !a.isBlocked(now) && a.windowExpired(now, window));
    }
}
