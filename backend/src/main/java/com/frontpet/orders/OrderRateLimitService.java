package com.frontpet.orders;

import com.frontpet.common.SlidingWindowLimiter;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Contador de requests a {@code POST /api/v1/orders} por IP, en memoria
 * (tarea 4.15). Envuelve {@link SlidingWindowLimiter} (núcleo compartido con
 * {@code identity.LoginAttemptService} y {@code booking.AppointmentRateLimitService}
 * desde la unificación de {@code docs/pending-decisions.md} §9).
 *
 * <p>A diferencia del login, acá se cuenta <b>toda</b> request, exitosa o no
 * ({@code recordRequest}): un checkout público sin pago no tiene un "fallo"
 * claro que distinga tráfico legítimo de abuso, así que limitar el volumen
 * bruto es la única señal disponible. No hay reset por éxito.
 */
@Service
public class OrderRateLimitService {

    private final SlidingWindowLimiter limiter;

    public OrderRateLimitService(Clock clock, OrderRateLimitProperties props) {
        this.limiter = new SlidingWindowLimiter(clock, props.enabled(), props.maxRequests(), props.window());
    }

    /** @return segundos que faltan para poder reintentar, o 0 si no hay bloqueo. */
    public long blockedSecondsRemaining(String key) {
        return limiter.blockedSecondsRemaining(key);
    }

    /**
     * Registra una request (exitosa o no). A diferencia de
     * {@code LoginAttemptService.recordFailure}, no hay reset por éxito: acá
     * no existe la noción de "éxito" que blanquee el contador.
     */
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
