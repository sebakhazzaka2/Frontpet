package com.frontpet.catalog;

/**
 * El producto pedido no existe, no pertenece al tenant, o está inactivo.
 *
 * <p>Los tres casos comparten excepción a propósito: desde afuera son el mismo
 * 404. Distinguir "no existe" de "es de otro tenant" le confirmaría a un
 * curioso que el producto existe en otra cuenta.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
