package com.frontpet.notifications;

/** Mensaje de email agnóstico del proveedor. */
public record EmailMessage(String to, String subject, String textBody, String htmlBody) {
}
