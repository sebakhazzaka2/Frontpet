package com.frontpet.booking.dto;

import com.frontpet.booking.domain.ServiceType;

import java.math.BigDecimal;

/**
 * Proyección de {@code services JOIN service_pricing} para un porte —
 * resuelve tipo, precio y duración de un servicio en una sola query
 * (ADR 020, paso 3). Trae el {@code type} para poder validar en la capa de
 * servicio que el base es {@code BASE} y los adicionais son {@code ADDON}
 * sin una segunda consulta.
 */
public record ServicePriceLine(
        Long serviceId,
        ServiceType type,
        BigDecimal price,
        int durationMinutes
) {
}
