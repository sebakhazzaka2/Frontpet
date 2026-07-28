package com.frontpet.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Variante al crear un producto. Solo en {@code POST} — el {@code PUT} no
 * toca variantes (ver docs/pending-decisions.md §5: la FK real de
 * {@code order_items} a {@code product_variants} hace que "reemplazar el
 * array completo" sea riesgoso una vez que existan pedidos reales).
 *
 * <p>{@code price} es {@code @NotNull} a diferencia del producto: una
 * variante siempre tiene precio propio (V2, columna {@code NOT NULL}), nunca
 * lo hereda del padre.
 */
public record ProductVariantRequest(
        @NotBlank String nomeVariante,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero Integer stock
) {
}
