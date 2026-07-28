package com.frontpet.orders.domain;

/**
 * Forma de pagamento capturada en el checkout — dato operativo para que el
 * petshop sepa cómo cobrar al entregar, NUNCA una pasarela de pago online
 * (CLAUDE.md §6, ADR 003 act. 2026-07-09).
 *
 * <p>{@code V4__orders.sql:25} guarda esto en un {@code VARCHAR(40)} sin
 * {@code CHECK} ("valores TBD"): la lista cerrada vive acá, en el enum del
 * contrato, no en la DB — así se puede ajustar sin migración hasta que el
 * cliente confirme el set definitivo.
 */
public enum FormaPagamento {
    DINHEIRO,
    PIX,
    CARTAO_DEBITO,
    CARTAO_CREDITO
}
