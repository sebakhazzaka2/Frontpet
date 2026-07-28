package com.frontpet.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de pedido tal como se muestra en el detalle (checkout response, admin).
 * Los campos {@code *Snapshot} son los que congeló {@code order_items} al
 * crearse — nunca reflejan el precio/nombre actual del producto en catálogo
 * (ADR 013 §5). {@code variantId} es el id interno {@code Long} de
 * {@code product_variants} (no tienen {@code public_id}, ver
 * {@code CreateOrderItemRequest}), {@code null} si el item no tiene variante.
 */
public record OrderItemDetail(
        UUID productPublicId,
        Long variantId,
        String nomeSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantidade
) {
    public BigDecimal subtotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantidade));
    }
}
