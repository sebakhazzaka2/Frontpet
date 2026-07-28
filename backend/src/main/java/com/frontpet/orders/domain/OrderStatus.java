package com.frontpet.orders.domain;

/**
 * Estados del pedido (ADR 003, act. 2026-07-09). {@code PENDING_WHATSAPP}
 * quedó derogado — no existe acá, aunque siga apareciendo como drift en el
 * código de ejemplo del ADR 010 (a corregir en el PR de docs del sprint).
 *
 * <p>{@code CLAUDE.md} §6: no hay estados más allá de estos tres.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
