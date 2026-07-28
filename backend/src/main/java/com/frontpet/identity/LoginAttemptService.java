package com.frontpet.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Contador de intentos fallidos de login, en memoria (tarea 1.8, ADR 019).
 *
 * <p>En memoria y no en DB: una sola instancia en un solo VPS (ADR 016), y un
 * write a Postgres por login fallido sería amplificar en la DB un tráfico que
 * el atacante controla. Un reinicio borra los contadores — bypass real pero
 * acotado, ver ADR 019.
 *
 * <p>La clave es un {@code String} opaco a propósito ({@code "ip:203.0.113.7"}):
 * agnóstica de qué dimensión bloquea, para que si en la tarea 4.15 hace falta
 * la misma lógica sobre otra clave, se reuse tal cual.
 */
@Service
public class LoginAttemptService {

    /**
     * Tope duro de claves rastreadas. Un mapa sin límite indexado por IP es,
     * él mismo, un vector de DoS por memoria. 10k × ~100 bytes ≈ 1 MB.
     * Package-private (no {@code private}) para que el test de eviction no
     * duplique el número.
     */
    static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final LoginRateLimitProperties props;

    public LoginAttemptService(Clock clock, LoginRateLimitProperties props) {
        this.clock = clock;
        this.props = props;
    }

    private record Attempt(int failures, Instant windowStartedAt, Instant blockedUntil) {

        boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        boolean windowExpired(Instant now, Duration window) {
            return now.isAfter(windowStartedAt.plus(window));
        }
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        if (!props.enabled()) {
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
     * Registra un fallo. Al llegar a {@code maxFailures} dentro de la
     * ventana, bloquea la clave hasta que la ventana vuelva a vencer.
     */
    public void recordFailure(String key) {
        if (!props.enabled()) {
            return;
        }
        sweepIfNeeded();
        Instant now = clock.instant();
        attempts.compute(key, (k, prev) -> {
            if (prev == null || prev.windowExpired(now, props.window())) {
                return new Attempt(1, now, null);
            }
            int failures = prev.failures() + 1;
            Instant blockedUntil = failures >= props.maxFailures()
                    ? now.plus(props.window())
                    : null;
            return new Attempt(failures, prev.windowStartedAt(), blockedUntil);
        });
    }

    /** Un login exitoso limpia el historial de esa clave. */
    public void reset(String key) {
        attempts.remove(key);
    }

    /**
     * Solo para tests: el bean es singleton y Spring cachea el
     * {@code ApplicationContext} entre clases de test, así que el estado se
     * filtra de una clase a otra sin esto.
     */
    void clear() {
        attempts.clear();
    }

    /** Solo para tests: verificar que el barrido perezoso acota el mapa. */
    int trackedKeyCount() {
        return attempts.size();
    }

    /**
     * Barrido perezoso al pasar el tope de claves rastreadas. Sin
     * {@code @Scheduled}: no vale prender un thread pool para una sola
     * operación en un endpoint de tráfico ínfimo como el login.
     */
    private void sweepIfNeeded() {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        Instant now = clock.instant();
        attempts.values().removeIf(a -> !a.isBlocked(now) && a.windowExpired(now, props.window()));
    }
}
