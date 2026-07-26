package com.frontpet.catalog.dto;

import java.math.BigDecimal;

/**
 * Producto tal como se ve en el listado del catálogo (la tarjeta de la grilla).
 *
 * <p>Lo construye directamente la query de {@code ProductRepository} — nunca se
 * arma a partir de una entidad cargada. Eso es a propósito: sin entidades no hay
 * colecciones lazy que puedan explotar fuera de la transacción, y la paginación
 * la resuelve Postgres en vez de Hibernate en memoria.
 *
 * @param price      precio a mostrar. Si el producto tiene variantes, es el de
 *                   la variante activa más barata
 * @param hasVariants si es true, el frontend muestra "a partir de R$ X"
 */
public record ProductSummary(
        String slug,
        String nome,
        String mainImageUrl,
        BigDecimal price,
        BigDecimal priceOriginal,
        String brandNome,
        boolean hasVariants
) {
    /** Hay promoción cuando existe un precio anterior más alto para tachar. */
    public boolean isOnSale() {
        return priceOriginal != null;
    }
}
