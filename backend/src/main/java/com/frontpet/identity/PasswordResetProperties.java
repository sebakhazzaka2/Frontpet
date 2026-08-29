package com.frontpet.identity;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config del reset de contraseña del admin (tarea 7.12, ADR 022): TTL del
 * token de un solo uso + los dos rate limits que lo protegen (IP y email —
 * ver el javadoc de {@link PasswordResetEmailAttemptService} para el porqué
 * de no compartir un único límite entre ambos).
 *
 * @param ttl           vigencia del token desde que se genera. 60 min:
 *                       15 min es hostil si el mail tarda, 24h es ventana
 *                       de ataque gratis
 * @param ipRateLimit    cubre POST /forgot-password Y /reset-password
 * @param emailRateLimit solo POST /forgot-password (el filtro no puede leer
 *                       el body — el chequeo corre dentro del service)
 */
@ConfigurationProperties("frontpet.password-reset")
public record PasswordResetProperties(Duration ttl, RateLimit ipRateLimit, RateLimit emailRateLimit) {

    /**
     * @param enabled     escape hatch: {@code false} desactiva el límite sin tocar código
     * @param maxAttempts pedidos permitidos dentro de {@code window} antes de bloquear
     * @param window      ventana deslizante de conteo
     */
    public record RateLimit(boolean enabled, int maxAttempts, Duration window) {
    }
}
