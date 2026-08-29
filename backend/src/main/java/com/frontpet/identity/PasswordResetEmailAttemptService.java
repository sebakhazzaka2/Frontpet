package com.frontpet.identity;

import com.frontpet.common.SlidingWindowLimiter;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Rate limit por email de {@code POST /forgot-password} (tarea 7.12).
 * Wrapper propio, no compartido con {@code PasswordResetIpAttemptService}:
 * protegen cosas distintas con umbrales que no tienen por qué coincidir
 * (email 3/1h, IP 5/15min) — mismo criterio de no compartir código entre
 * rate limits que {@code LoginAttemptService} vs. {@code OrderRateLimitService}.
 *
 * <p>A diferencia de {@code LoginAttemptService}, cuenta TODO pedido, no solo
 * los "fallidos": {@code forgot-password} siempre devuelve 202, no hay una
 * noción de fallo observable desde afuera (modelo de
 * {@code OrderRateLimitService}, no el del login).
 *
 * <p>Vive fuera de un filtro HTTP y se consulta desde
 * {@link PasswordResetService} directamente: un {@code OncePerRequestFilter}
 * no puede leer el body del request para sacar el email sin envolverlo — por
 * eso el rate limit de IP de {@code PasswordResetRateLimitFilter} es el único
 * que corre a nivel filtro.
 */
@Service
public class PasswordResetEmailAttemptService {

    private final SlidingWindowLimiter limiter;

    public PasswordResetEmailAttemptService(Clock clock, PasswordResetProperties props) {
        this.limiter = new SlidingWindowLimiter(
                clock,
                props.emailRateLimit().enabled(),
                props.emailRateLimit().maxAttempts(),
                props.emailRateLimit().window());
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        return limiter.blockedSecondsRemaining(key);
    }

    /** Registra un pedido. Al llegar al límite dentro de la ventana, bloquea la clave. */
    public void recordAttempt(String key) {
        limiter.recordAttempt(key);
    }

    /** Un reset exitoso libera el presupuesto de esa clave. */
    public void reset(String key) {
        limiter.reset(key);
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
