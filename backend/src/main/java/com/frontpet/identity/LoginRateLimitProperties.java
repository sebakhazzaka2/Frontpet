package com.frontpet.identity;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Límites del rate limit del login (tarea 1.8, ADR 019). Solo por IP — el
 * porqué de no bloquear también por cuenta (auto-DoS con un solo admin en
 * MVP1) está documentado en el ADR, no acá.
 *
 * @param enabled     escape hatch: {@code false} desactiva el rate limit sin
 *                    tocar código, para rescatar a alguien con un redeploy
 * @param maxFailures fallos permitidos por IP dentro de {@code window} antes
 *                    de bloquear
 * @param window      ventana deslizante de conteo
 */
@ConfigurationProperties("frontpet.login-rate-limit")
public record LoginRateLimitProperties(
        boolean enabled,
        int maxFailures,
        Duration window) {
}
