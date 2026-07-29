package com.frontpet.catalog.dto;

import java.math.BigDecimal;

/**
 * Lo que {@code orders} congela como snapshot al crear un pedido.
 *
 * @param productId  id interno del producto ({@code order_items.product_id}).
 *                   No es {@code publicId}: distinto de {@link ProductDetail},
 *                   que a propósito no expone el id interno afuera de catalog
 * @param variantId  id interno de la variante elegida, o {@code null} si el
 *                   producto no tiene variantes
 * @param unitPrice  precio que manda — el de la variante si hay, si no el del
 *                   producto. Nunca el que mandó el cliente en el request
 */
public record OrderLineSnapshot(
        Long productId,
        Long variantId,
        String nomeSnapshot,
        BigDecimal unitPrice
) {
}
