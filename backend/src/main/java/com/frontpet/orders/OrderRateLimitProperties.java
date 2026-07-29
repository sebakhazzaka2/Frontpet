package com.frontpet.orders;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites del rate limit de {@code POST /api/v1/orders} (tarea 4.15).
 *
 * <p>Deliberadamente separado de {@code identity.LoginRateLimitProperties}
 * aunque la forma sea casi idéntica — ver el javadoc de
 * {@link OrderRateLimitService} para el porqué de no compartir código.
 *
 * @param enabled      escape hatch: {@code false} desactiva el rate limit sin
 *                     tocar código
 * @param maxRequests  requests permitidos por IP dentro de {@code window}
 *                     antes de bloquear — a diferencia del login, cuenta
 *                     TODAS las requests, no solo las fallidas
 * @param window       ventana deslizante de conteo
 */
@ConfigurationProperties("frontpet.order-rate-limit")
public record OrderRateLimitProperties(
        boolean enabled,
        int maxRequests,
        Duration window) {
}
