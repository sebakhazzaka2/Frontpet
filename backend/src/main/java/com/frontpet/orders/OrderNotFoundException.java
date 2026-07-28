package com.frontpet.orders;

/**
 * El pedido pedido no existe, no pertenece al tenant, o pertenece a otro
 * (mismo criterio que {@code catalog.ProductNotFoundException}: desde afuera
 * los tres casos son el mismo 404, para no confirmarle a un curioso que un
 * {@code publicId} existe en otra cuenta).
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
