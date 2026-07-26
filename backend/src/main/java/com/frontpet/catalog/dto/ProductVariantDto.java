package com.frontpet.catalog.dto;

import java.math.BigDecimal;

/**
 * Una presentación concreta del producto: "10 kg", "60 unidades".
 *
 * @param id identificador interno (BIGSERIAL). Las variantes no tienen
 *           {@code public_id} — el ADR 013 §1 solo se lo da a products,
 *           orders y appointments. El carrito referencia la variante por
 *           este id, igual que hace {@code order_items} en la DB.
 */
public record ProductVariantDto(
        Long id,
        String nomeVariante,
        BigDecimal price,
        BigDecimal priceOriginal,
        Integer stock
) {
    public boolean isOnSale() {
        return priceOriginal != null;
    }

    public boolean isInStock() {
        return stock != null && stock > 0;
    }
}
