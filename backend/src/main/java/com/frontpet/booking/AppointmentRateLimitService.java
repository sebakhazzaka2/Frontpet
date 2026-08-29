package com.frontpet.booking;

import com.frontpet.common.SlidingWindowLimiter;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Contador de requests a {@code POST /api/v1/appointments} por IP, en memoria
 * (tarea 5.8). Envuelve {@link SlidingWindowLimiter} (núcleo compartido con
 * {@code identity.LoginAttemptService} y {@code orders.OrderRateLimitService}
 * desde la unificación de {@code docs/pending-decisions.md} §9). Misma
 * semántica que {@code OrderRateLimitService}: cuenta toda request, sin reset
 * por éxito.
 */
@Service
public class AppointmentRateLimitService {

    private final SlidingWindowLimiter limiter;

    public AppointmentRateLimitService(Clock clock, AppointmentRateLimitProperties props) {
        this.limiter = new SlidingWindowLimiter(clock, props.enabled(), props.maxRequests(), props.window());
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        return limiter.blockedSecondsRemaining(key);
    }

    /** Registra una request (exitosa o no) — no hay noción de "éxito" que resetee el contador. */
    public void recordRequest(String key) {
        limiter.recordAttempt(key);
    }

    /** Solo para tests: el bean es singleton y Spring cachea el ApplicationContext entre clases. */
    void clear() {
        limiter.clear();
    }

    /** Solo para tests: verificar que el barrido perezoso acota el mapa. */
    int trackedKeyCount() {
        return limiter.trackedKeyCount();
    }
}
