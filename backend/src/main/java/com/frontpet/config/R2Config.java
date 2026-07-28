package com.frontpet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Cliente de firma de URLs para Cloudflare R2 (S3-compatible), tarea 3.5.
 *
 * <p>Solo un {@link S3Presigner}, no un {@code S3Client}: el backend nunca
 * sube ni baja bytes de R2 — firma la URL y el browser hace el PUT directo.
 * {@code region("auto")} y {@code pathStyleAccessEnabled(true)} son los dos
 * ajustes no estándar que pide R2 para andar con el SDK de AWS (confirmado
 * contra la doc oficial de Cloudflare, 2026-07-27).
 */
@Configuration
public class R2Config {

    /**
     * Placeholders sintácticamente válidos para dev/test sin credenciales
     * reales. {@code S3Presigner.build()} exige una URI con scheme y
     * rechaza credenciales en blanco ({@code "Access key ID cannot be
     * blank"}) — con los defaults vacíos de {@code application.yml} fuera de
     * prod, eso tumbaba el arranque de cualquiera que corriera el backend
     * sin las env vars de R2 configuradas (encontrado corriendo la suite
     * completa, 2026-07-27: pasaba local pero rompía todo test
     * {@code @SpringBootTest}). {@link StartupEnvValidator} sigue exigiendo
     * los valores reales en producción; esto es solo para no tumbar el
     * contexto de Spring en dev/test — nadie va a firmar una URL real con
     * estos valores fuera de un test que mockee el presigner.
     */
    private static final String DEV_PLACEHOLDER_ENDPOINT = "https://r2-not-configured.invalid";
    private static final String DEV_PLACEHOLDER_ACCESS_KEY_ID = "dev-not-configured";
    private static final String DEV_PLACEHOLDER_SECRET_ACCESS_KEY = "dev-not-configured";

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String endpoint;

    public R2Config(@Value("${frontpet.r2.access-key-id}") String accessKeyId,
                    @Value("${frontpet.r2.secret-access-key}") String secretAccessKey,
                    @Value("${frontpet.r2.endpoint}") String endpoint) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.endpoint = endpoint;
    }

    @Bean
    public S3Presigner s3Presigner() {
        String effectiveEndpoint = endpoint.isBlank() ? DEV_PLACEHOLDER_ENDPOINT : endpoint;
        String effectiveAccessKeyId = accessKeyId.isBlank() ? DEV_PLACEHOLDER_ACCESS_KEY_ID : accessKeyId;
        String effectiveSecretAccessKey = secretAccessKey.isBlank()
                ? DEV_PLACEHOLDER_SECRET_ACCESS_KEY : secretAccessKey;

        return S3Presigner.builder()
                .endpointOverride(URI.create(effectiveEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(effectiveAccessKeyId, effectiveSecretAccessKey)))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
