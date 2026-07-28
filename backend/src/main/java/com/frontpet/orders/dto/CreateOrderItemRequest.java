package com.frontpet.orders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Item del pedido tal como lo manda el checkout.
 *
 * <p>Referencia el producto por {@code publicId}, no por {@code slug} (ADR 013,
 * act. 2026-07-25: el catálogo público usa slug para la URL, pero carrito y
 * pedidos referencian el identificador estable). {@code variantId} es
 * {@code null} si el producto no tiene variantes, y es el id interno
 * {@code Long} de {@code product_variants} — las variantes NO tienen
 * {@code public_id} (ADR 013 §1 solo se lo da a products/orders/appointments;
 * ver javadoc de {@code ProductVariantDto.id}), así que el carrito y
 * {@code order_items} las referencian igual, por este id.
 *
 * <p>No lleva precio ni nombre: el backend los recalcula siempre contra
 * {@code ProductService.getByPublicId} y los congela en {@code order_items}
 * como snapshot. Nunca se confía el precio que manda el cliente.
 */
public record CreateOrderItemRequest(
        @NotNull UUID productPublicId,
        Long variantId,
        @NotNull @Positive Integer quantidade
) {
}
