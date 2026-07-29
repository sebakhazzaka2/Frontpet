package com.frontpet.orders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Contador de requests a {@code POST /api/v1/orders} por IP, en memoria
 * (tarea 4.15).
 *
 * <p><b>Deliberadamente duplica</b> la mecánica de
 * {@code identity.LoginAttemptService} en vez de extraerla a {@code common/}
 * (CLAUDE.md §6: no hay abstracciones genéricas hasta tener 3 casos de uso
 * reales — hoy hay 2, login y este). La semántica además difiere: el login
 * consume presupuesto solo en los intentos <b>fallidos</b>
 * ({@code recordFailure}); acá se consume en <b>cada</b> request, exitosa o
 * no ({@code recordRequest}) — un checkout público sin pago no tiene un
 * "fallo" claro que distinga tráfico legítimo de abuso, así que limitar el
 * volumen bruto es la única señal disponible.
 *
 * <p>Deuda explícita: cuando aparezca un tercer caso (candidato: booking,
 * Sprint 6) es el momento de unificar en un limitador genérico por clave
 * opaca. Registrado en {@code docs/pending-decisions.md} por el Bloque G.
 */
@Service
public class OrderRateLimitService {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final OrderRateLimitProperties props;

    public OrderRateLimitService(Clock clock, OrderRateLimitProperties props) {
        this.clock = clock;
        this.props = props;
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
     * Registra una request (exitosa o no). A diferencia de
     * {@code LoginAttemptService.recordFailure}, no hay reset por éxito: acá
     * no existe la noción de "éxito" que blanquee el contador.
     */
    public void recordRequest(String key) {
        if (!props.enabled()) {
            return;
        }
        sweepIfNeeded();
        Instant now = clock.instant();
        attempts.compute(key, (k, prev) -> {
            if (prev == null || prev.windowExpired(now, props.window())) {
                return new Attempt(1, now, null);
            }
            int count = prev.count() + 1;
            Instant blockedUntil = count >= props.maxRequests()
                    ? now.plus(props.window())
                    : null;
            return new Attempt(count, prev.windowStartedAt(), blockedUntil);
        });
    }

    /** Solo para tests: el bean es singleton y Spring cachea el ApplicationContext entre clases. */
    void clear() {
        attempts.clear();
    }

    /** Solo para tests: verificar que el barrido perezoso acota el mapa. */
    int trackedKeyCount() {
        return attempts.size();
    }

    private void sweepIfNeeded() {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        Instant now = clock.instant();
        attempts.values().removeIf(a -> !a.isBlocked(now) && a.windowExpired(now, props.window()));
    }
}
