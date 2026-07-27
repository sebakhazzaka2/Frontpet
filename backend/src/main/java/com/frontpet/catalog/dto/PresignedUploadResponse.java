package com.frontpet.catalog.dto;

import java.time.Instant;

/**
 * @param uploadUrl URL firmada donde el browser hace el PUT directo a R2.
 * @param publicUrl URL final de lectura — es la que el admin manda como
 *                  {@code mainImageUrl} al crear/editar el producto, después
 *                  de confirmar que el upload terminó.
 * @param objectKey key del objeto en el bucket, informativo.
 */
public record PresignedUploadResponse(
        String uploadUrl,
        String publicUrl,
        String objectKey,
        Instant expiresAt
) {
}
