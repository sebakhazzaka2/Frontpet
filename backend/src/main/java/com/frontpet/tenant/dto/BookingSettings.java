package com.frontpet.tenant.dto;

/**
 * Config de booking leída de {@code tenants.config} (JSONB, ADR 009/012).
 * Sembrada hoy en {@code V5__seed_dev.sql}.
 */
public record BookingSettings(
        int capacidadeAtendimento,
        int anticipacaoMinHoras,
        int anticipacaoMaxDias
) {
}
