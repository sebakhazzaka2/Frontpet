package com.frontpet.catalog;

import com.frontpet.catalog.dto.PresignedUploadRequest;
import com.frontpet.catalog.dto.PresignedUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test puro (sin Spring, sin red): {@link S3Presigner#presignPutObject}
 * es una operación local (calcula la firma SigV4, no llama a R2), así que la
 * validación de mime/tamaño y el shape de la respuesta se pueden probar sin
 * levantar contexto ni credenciales reales.
 */
class R2StorageServiceImplTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");
    private static final String BUCKET = "frontpet-products-test";
    private static final String PUBLIC_URL = "https://cdn.frontpet.test";

    private R2StorageServiceImpl service;

    @BeforeEach
    void setUp() {
        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create("https://fake-account.r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fake-access-key", "fake-secret-key")))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        service = new R2StorageServiceImpl(presigner, BUCKET, PUBLIC_URL);
    }

    @Test
    @DisplayName("mime não permitido rejeita com IllegalArgumentException")
    void rejectsDisallowedMimeType() {
        PresignedUploadRequest request = new PresignedUploadRequest("foto.pdf", "application/pdf", 1024L);

        assertThatThrownBy(() -> service.presignProductImageUpload(TENANT, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de imagem não permitido");
    }

    @Test
    @DisplayName("tamanho acima do tope rejeita com IllegalArgumentException")
    void rejectsOversizedFile() {
        long overTheLimit = 6L * 1024 * 1024; // 6MB > tope de 5MB
        PresignedUploadRequest request = new PresignedUploadRequest("foto.jpg", "image/jpeg", overTheLimit);

        assertThatThrownBy(() -> service.presignProductImageUpload(TENANT, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamanho máximo");
    }

    @Test
    @DisplayName("request válido devuelve URL firmada, publicUrl e key com extensão correta")
    void presignsValidRequest() {
        PresignedUploadRequest request = new PresignedUploadRequest("foto.png", "image/png", 500_000L);

        PresignedUploadResponse response = service.presignProductImageUpload(TENANT, request);

        assertThat(response.uploadUrl()).startsWith("https://fake-account.r2.cloudflarestorage.com");
        assertThat(response.objectKey()).startsWith("products/" + TENANT + "/").endsWith(".png");
        assertThat(response.publicUrl()).isEqualTo(PUBLIC_URL + "/" + response.objectKey());
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("cada request genera una key distinta (sin colisiones entre uploads)")
    void generatesUniqueKeysPerRequest() {
        PresignedUploadRequest request = new PresignedUploadRequest("foto.webp", "image/webp", 200_000L);

        PresignedUploadResponse first = service.presignProductImageUpload(TENANT, request);
        PresignedUploadResponse second = service.presignProductImageUpload(TENANT, request);

        assertThat(first.objectKey()).isNotEqualTo(second.objectKey());
    }
}
