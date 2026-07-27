package com.frontpet.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Alta de producto, con o sin variantes (AC de la tarea 3.4).
 *
 * <p>{@code price}/{@code stock} solo aplican cuando {@code variants} viene
 * vacío — la invariante "sin variantes exige precio" no se expresa acá con
 * anotaciones (es una regla cruzada entre dos campos, no de uno solo) sino en
 * {@code ProductServiceImpl}, donde ya vive el resto de la lógica de negocio
 * del producto.
 *
 * <p>{@code slug} no está acá: se genera siempre desde {@code nome} al crear
 * ({@code ProductService.generateUniqueSlug}), igual que ya hacía el service.
 * Editarlo es un caso de {@code UpdateProductRequest}, no de la creación.
 */
public record CreateProductRequest(
        @NotBlank String nome,
        String descricao,
        String mainImageUrl,
        @PositiveOrZero BigDecimal price,
        @PositiveOrZero Integer stock,
        String brandNome,
        List<String> categorySlugs,
        List<String> speciesSlugs,
        @Valid List<ProductVariantRequest> variants
) {
}
