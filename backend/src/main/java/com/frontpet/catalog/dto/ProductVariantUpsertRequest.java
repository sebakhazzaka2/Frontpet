package com.frontpet.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Variante en {@code PUT /admin/products/{id}/variants} (tarea 3.4b —
 * docs/pending-decisions.md §5).
 *
 * <p>{@code id == null}: variante nueva. {@code id != null}: actualiza la
 * existente con ese id — tiene que pertenecer al producto del path, si no
 * {@code ProductServiceImpl.replaceVariants} rechaza con 400 (evita que un
 * admin adivine ids de otro producto).
 */
public record ProductVariantUpsertRequest(
        Long id,
        @NotBlank String nomeVariante,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero Integer stock
) {
}
