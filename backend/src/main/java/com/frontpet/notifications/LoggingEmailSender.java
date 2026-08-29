package com.frontpet.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación de dev/test: escribe el email en el log en vez de mandarlo
 * de verdad. Única forma de probar el flujo de reset en local sin una API
 * key real de Resend — {@code StartupEnvValidator} impide que esto se
 * seleccione en producción (exige {@code RESEND_API_KEY}).
 *
 * <p>Única excepción deliberada y acotada a "nunca loguear el token en claro"
 * (ver {@code PasswordResetService}): sin escribir el link con el token acá
 * no hay forma de probar el flujo completo en local.
 */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.warn("=== E-MAIL SIMULADO (RESEND_API_KEY não configurada) ===");
        log.warn("Para: {}", message.to());
        log.warn("Assunto: {}", message.subject());
        log.warn("Corpo (texto):\n{}", message.textBody());
    }
}
