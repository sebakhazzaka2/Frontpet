package com.frontpet.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Pedido de URL firmada para subir la foto de un producto (tarea 3.5).
 *
 * <p>{@code contentLength} lo declara el cliente (tamaño real del archivo
 * elegido) — el service lo valida contra el tope antes de firmar, pero es un
 * chequeo sobre lo declarado, no una garantía de R2 sobre lo realmente
 * subido (ver {@code docs/pending-decisions.md}).
 */
public record PresignedUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long contentLength
) {
}
