package com.frontpet.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selección de {@link EmailSender} (tarea 7.12): mismo precedente que
 * {@code config.R2Config} con los placeholders de {@code S3Presigner} — sin
 * API key real, dev/test usan un placeholder en vez de romper el arranque.
 */
@Configuration
public class NotificationsConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationsConfig.class);

    /** Bean compartido, no uno por envío: {@code sendAsync} reusa su pool interno de conexiones/hilos. */
    @Bean
    public HttpClient emailHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public EmailSender emailSender(
            HttpClient emailHttpClient,
            ObjectMapper objectMapper,
            @Value("${frontpet.resend.api-key}") String apiKey,
            @Value("${frontpet.resend.from}") String from) {
        if (apiKey.isBlank()) {
            log.warn("RESEND_API_KEY não configurada — usando LoggingEmailSender (e-mails não são enviados de "
                    + "verdade, só logados). StartupEnvValidator impede que isso aconteça em produção.");
            return new LoggingEmailSender();
        }
        return new ResendEmailSender(emailHttpClient, objectMapper, apiKey, from);
    }
}
