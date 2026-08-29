package com.frontpet.notifications;

/**
 * Puerto de salida para envío de email (tarea 7.12, primer consumidor de este
 * paquete). {@code send} nunca lanza — ver {@link ResendEmailSender} para el
 * porqué: un proveedor caído no puede cambiar la respuesta HTTP de quien
 * llama, o filtra información por timing/error (anti-enumeração de
 * {@code PasswordResetService}).
 */
public interface EmailSender {
    void send(EmailMessage message);
}
