package com.frontpet.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cliente HTTP a la API de Resend (tarea 7.12). Primer HTTP saliente del
 * repo: {@code java.net.http.HttpClient} del JDK 21 + el {@code ObjectMapper}
 * de Boot, sin dependencias nuevas — mismo criterio que {@code common.UuidV7}
 * (CLAUDE.md §6, "¿se resuelve con vanilla?"). Sienta el precedente para el
 * próximo HTTP saliente que aparezca.
 *
 * <p><b>{@code sendAsync}, no {@code send}</b>: por dos motivos. (1) Sin
 * async, un Resend colgado convierte cada {@code forgot-password} en 10s de
 * hilo bloqueado. (2) El envío tiene que ser asíncrono o la anti-enumeração
 * de {@code PasswordResetService} se rompe por timing — un email existente
 * respondería en ~300ms y uno inexistente en ~2ms.
 *
 * <p><b>Nunca propaga</b>: {@link #send} no lanza. Los errores se loguean en
 * ERROR; el caller sigue como si el envío hubiera salido. Eso es lo que
 * garantiza que un Resend caído no cambie la respuesta HTTP y por lo tanto no
 * filtre si un email existe. <b>Consecuencia aceptada</b>: con Resend caído
 * el token existe en la DB pero el usuario nunca lo recibe; el TTL de 60 min
 * lo vuelve irrelevante y el admin simplemente reintenta (ADR 022, mismo
 * criterio de documentar la limitación en vez de esconderla que ADR 018).
 *
 * <p><b>Sin reintentos</b>: un reintento sobre un endpoint de envío puede
 * duplicar el mail, y un segundo mail con otro token confunde más de lo que
 * ayuda.
 */
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String from;

    public ResendEmailSender(HttpClient httpClient, ObjectMapper objectMapper, String apiKey, String from) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", from,
                    "to", List.of(message.to()),
                    "subject", message.subject(),
                    "text", message.textBody(),
                    "html", message.htmlBody()));

            HttpRequest request = HttpRequest.newBuilder(RESEND_ENDPOINT)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            log.error("Falha ao enviar e-mail via Resend", error);
                        } else if (response.statusCode() >= 400) {
                            log.error("Resend respondeu {} ao enviar e-mail: {}",
                                    response.statusCode(), response.body());
                        }
                    });
        } catch (Exception e) {
            // Nunca propaga: ver el javadoc de la clase.
            log.error("Erro inesperado ao montar o envio de e-mail via Resend", e);
        }
    }
}
