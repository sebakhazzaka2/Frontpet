package com.frontpet.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Edición de producto (AC de la tarea 3.4). Reemplazo completo de los campos
 * editables — no parcial: {@code descricao}/{@code mainImageUrl} son
 * nullables donde {@code null} es un valor legítimo ("borrar la descripción"),
 * así que no hay forma de distinguir "no tocar" de "vaciar" con semántica de
 * parche. El admin manda el estado completo, como un formulario que se
 * guarda entero.
 *
 * <p>Excepción: {@code slug}. Es el único campo donde "vacío" significa "no
 * tocar" — nunca es válido que un producto quede sin slug, así que no hay
 * ambigüedad real en tratarlo distinto del resto.
 *
 * <p>Sin variantes — ver {@code ProductVariantRequest} y
 * docs/pending-decisions.md §5.
 *
 * <p>{@code priceOriginal}: a diferencia de {@code slug}, acá {@code null} SÍ
 * significa "vaciar" (sacar de promoção), no "no tocar" — coherente con el
 * resto del record, que es reemplazo completo. Antes de esto no existía
 * ninguna forma de sacar un produto de oferta vía API (docs/pending-decisions.md §3).
 */
public record UpdateProductRequest(
        @NotBlank String nome,
        String descricao,
        String mainImageUrl,
        @PositiveOrZero BigDecimal price,
        @PositiveOrZero BigDecimal priceOriginal,
        @PositiveOrZero Integer stock,
        String brandNome,
        List<String> categorySlugs,
        List<String> speciesSlugs,
        /** Vacío o {@code null}: no se toca el slug actual. */
        String slug
) {
}
