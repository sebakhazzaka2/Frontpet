package com.frontpet.booking;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites del rate limit de {@code POST /api/v1/appointments} (tarea 5.8).
 *
 * <p>Tercera copia del mismo trío (login, orders, ahora booking) —
 * deliberadamente sin unificar dentro de este sprint (ver
 * {@code OrderRateLimitService}, javadoc, y {@code docs/pending-decisions.md}):
 * unificar en el sprint más riesgoso del plan no es el momento.
 *
 * @param enabled      escape hatch: {@code false} desactiva el rate limit sin
 *                     tocar código
 * @param maxRequests  requests permitidos por IP dentro de {@code window}
 *                     antes de bloquear — cuenta TODAS las requests, no solo
 *                     las que terminan en 409
 * @param window       ventana deslizante de conteo
 */
@ConfigurationProperties("frontpet.appointment-rate-limit")
public record AppointmentRateLimitProperties(
        boolean enabled,
        int maxRequests,
        Duration window) {
}
