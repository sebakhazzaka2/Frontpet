package com.frontpet.catalog;

import com.frontpet.catalog.dto.PresignedUploadRequest;
import com.frontpet.catalog.dto.PresignedUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class R2StorageServiceImpl implements R2StorageService {

    /** Únicos formatos que aceptamos para foto de producto (tarea 3.5b). */
    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    /**
     * Tope acordado 2026-07-27. Es un chequeo sobre el {@code contentLength}
     * que declara el cliente al pedir la URL, no algo que R2 aplique sobre
     * el PUT real: la doc oficial de R2 confirma que soporta firmar
     * {@code Content-Type} (un PUT con otro tipo rompe la firma, sí lo
     * aplica) pero NO soporta {@code content-length-range} ni presigned POST
     * — a diferencia de S3, no hay forma de que el storage garantice el
     * tamaño máximo de lo que efectivamente se sube.
     *
     * <p>Se acepta igual porque el único que puede llegar a este endpoint es
     * quien ya tiene la cookie de sesión del admin (protegido por
     * {@code anyRequest().authenticated()}); con esa sesión comprometida hay
     * riesgos mayores que el tamaño de un upload. Ver
     * {@code docs/pending-decisions.md} para la opción de un chequeo real
     * post-upload (HeadObject + delete) si el modelo de amenaza cambia.
     */
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Duration URL_EXPIRATION = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String publicUrl;

    public R2StorageServiceImpl(S3Presigner s3Presigner,
                                @Value("${frontpet.r2.bucket-name}") String bucketName,
                                @Value("${frontpet.r2.public-url}") String publicUrl) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
    }

    @Override
    public PresignedUploadResponse presignProductImageUpload(UUID tenantId, PresignedUploadRequest request) {
        String extension = ALLOWED_MIME_TYPES.get(request.contentType());
        if (extension == null) {
            throw new IllegalArgumentException(
                    "Tipo de imagem não permitido: " + request.contentType()
                    + ". Use JPEG, PNG ou WebP.");
        }
        if (request.contentLength() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Imagem excede o tamanho máximo de " + (MAX_FILE_SIZE_BYTES / (1024 * 1024)) + "MB.");
        }

        // Key aleatoria: evita colisiones entre uploads simultáneos y no
        // filtra el nombre original del archivo elegido por el admin.
        String objectKey = "products/%s/%s.%s".formatted(tenantId, UUID.randomUUID(), extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                // Firmado en la política: un PUT real con otro Content-Type
                // rompe la firma (403) — esto sí lo aplica R2.
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(URL_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUploadResponse(
                presigned.url().toString(),
                publicUrl + "/" + objectKey,
                objectKey,
                Instant.now().plus(URL_EXPIRATION));
    }
}
