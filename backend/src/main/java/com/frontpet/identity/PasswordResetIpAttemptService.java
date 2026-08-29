package com.frontpet.identity;

import com.frontpet.common.SlidingWindowLimiter;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Rate limit por IP de {@code POST /forgot-password} y {@code /reset-password}
 * (tarea 7.12), consumido por {@link PasswordResetRateLimitFilter}. Protege
 * el server y el brute-force del token de reset en sí.
 *
 * <p>Wrapper propio, no compartido con {@link PasswordResetEmailAttemptService}
 * — ver su javadoc para el porqué de no unificar ambos límites.
 */
@Service
public class PasswordResetIpAttemptService {

    private final SlidingWindowLimiter limiter;

    public PasswordResetIpAttemptService(Clock clock, PasswordResetProperties props) {
        this.limiter = new SlidingWindowLimiter(
                clock,
                props.ipRateLimit().enabled(),
                props.ipRateLimit().maxAttempts(),
                props.ipRateLimit().window());
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        return limiter.blockedSecondsRemaining(key);
    }

    /** Registra una request (exitosa o no) — no hay noción de "éxito" que resetee el contador. */
    public void recordAttempt(String key) {
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
