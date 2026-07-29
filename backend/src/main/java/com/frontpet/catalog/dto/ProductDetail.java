package com.frontpet.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Producto completo, para la página {@code /produtos/{slug}}.
 *
 * <p>A diferencia de {@link ProductSummary}, este sí se arma desde la entidad
 * — pero <b>dentro de la transacción del service</b>, con las colecciones ya
 * traídas por el {@code @EntityGraph}. Para cuando el controller lo serializa
 * ya es un objeto plano de Java: no queda nada lazy que pueda fallar.
 *
 * @param publicId identificador estable. La URL usa el slug; esto es para que
 *                 el carrito y los pedidos referencien el producto sin depender
 *                 de un slug que el admin puede editar
 * @param price    null cuando el producto tiene variantes: ahí el precio vive
 *                 en cada una
 * @param active   agregado en el issue #33 (Bloque E): el form de edición del
 *                 admin necesita saber si el producto está oculto para poder
 *                 mostrar el toggle. En {@code /produtos/{slug}} (público)
 *                 siempre sale {@code true} — ese endpoint ya filtra inactivos
 *                 antes de llegar acá
 */
public record ProductDetail(
        UUID publicId,
        String slug,
        String nome,
        String descricao,
        String mainImageUrl,
        BigDecimal price,
        BigDecimal priceOriginal,
        Integer stock,
        String brandNome,
        List<TaxonRef> categories,
        List<TaxonRef> species,
        List<ProductVariantDto> variants,
        boolean active
) {
    public boolean hasVariants() {
        return !variants.isEmpty();
    }

    public boolean isOnSale() {
        return priceOriginal != null;
    }
}
