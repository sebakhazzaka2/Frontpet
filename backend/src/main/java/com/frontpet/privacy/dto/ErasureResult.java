package com.frontpet.privacy.dto;

/** Resposta de {@code POST /api/v1/admin/privacy/anonymize}. */
public record ErasureResult(
        int ordersAnonymized,
        int appointmentsAnonymized
) {
}
