package com.frontpet.identity;

import com.frontpet.common.SlidingWindowLimiter;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Contador de intentos fallidos de login, en memoria (tarea 1.8, ADR 019).
 * Envuelve {@link SlidingWindowLimiter} (núcleo compartido con
 * {@code orders.OrderRateLimitService} y {@code booking.AppointmentRateLimitService}
 * desde la unificación de {@code docs/pending-decisions.md} §9) y le agrega la
 * semántica propia del login: solo los <b>fallos</b> cuentan, y un éxito
 * resetea el historial — lo único que este módulo no comparte con los otros
 * dos, que cuentan toda request y no tienen noción de "éxito".
 *
 * <p>La clave es un {@code String} opaco a propósito ({@code "ip:203.0.113.7"}):
 * agnóstica de qué dimensión bloquea.
 */
@Service
public class LoginAttemptService {

    /** Package-private: {@link LoginAttemptServiceTest} lo referencia por nombre histórico. */
    static final int MAX_TRACKED_KEYS = SlidingWindowLimiter.MAX_TRACKED_KEYS;

    private final SlidingWindowLimiter limiter;

    public LoginAttemptService(Clock clock, LoginRateLimitProperties props) {
        this.limiter = new SlidingWindowLimiter(clock, props.enabled(), props.maxFailures(), props.window());
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        return limiter.blockedSecondsRemaining(key);
    }

    /**
     * Registra un fallo. Al llegar a {@code maxFailures} dentro de la
     * ventana, bloquea la clave hasta que la ventana vuelva a vencer.
     */
    public void recordFailure(String key) {
        limiter.recordAttempt(key);
    }

    /** Un login exitoso limpia el historial de esa clave. */
    public void reset(String key) {
        limiter.reset(key);
    }

    /**
     * Solo para tests: el bean es singleton y Spring cachea el
     * {@code ApplicationContext} entre clases de test, así que el estado se
     * filtra de una clase a otra sin esto.
     */
    void clear() {
        limiter.clear();
    }

    /** Solo para tests: verificar que el barrido perezoso acota el mapa. */
    int trackedKeyCount() {
        return limiter.trackedKeyCount();
    }
}
