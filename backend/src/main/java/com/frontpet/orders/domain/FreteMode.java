package com.frontpet.orders.domain;

/**
 * Modalidad de frete (ADR 003, act. 2026-07-09). Nunca un valor numérico —
 * {@code Total = Subtotal} siempre.
 *
 * <p>{@code GRATIS}: entrega hasta 5km, o retirada en la loja (drift #1 del
 * plan de Sprint 4 — Stitch ofrece "Retirada" como modalidad de entrega, que
 * no tiene columna propia en {@code V4__orders.sql}; se persiste como
 * {@code GRATIS} + {@code endereco_entrega = "Retirada na loja"}).
 * {@code A_COMBINAR}: entrega a más de 5km.
 */
public enum FreteMode {
    GRATIS,
    A_COMBINAR
}
