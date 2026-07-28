package com.frontpet.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Producto tal como se ve en el listado del catálogo (la tarjeta de la grilla).
 *
 * <p>Lo construye directamente la query de {@code ProductRepository} — nunca se
 * arma a partir de una entidad cargada. Eso es a propósito: sin entidades no hay
 * colecciones lazy que puedan explotar fuera de la transacción, y la paginación
 * la resuelve Postgres en vez de Hibernate en memoria.
 *
 * @param publicId    identificador estable del producto (agregado en el Bloque A
 *                    del Sprint 4, issue #29): sin esto, el botón "Adicionar à
 *                    sacola" de la grilla no tiene forma de decirle a
 *                    {@code useCart()} qué producto agregar — hasta acá solo
 *                    {@link ProductDetail} lo exponía
 * @param price      precio a mostrar. Si el producto tiene variantes, es el de
 *                   la variante activa más barata
 * @param hasVariants si es true, el frontend muestra "a partir de R$ X" y no
 *                    ofrece agregar al carrito desde la grilla — hace falta
 *                    elegir variante primero, en {@code /produtos/{slug}}
 */
public record ProductSummary(
        UUID publicId,
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
