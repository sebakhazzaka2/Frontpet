package com.frontpet.booking.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Combo resuelto: banho base + N adicionais para un porte (ADR 011).
 *
 * <p>Público a propósito: {@code AppointmentServiceImpl} (Bloque E) necesita
 * exactamente el mismo cálculo para congelar los snapshots al crear el turno.
 * Recalcularlo ahí por separado sería la vía más corta a que disponibilidad y
 * reserva discrepen.
 *
 * <p>{@code totalDurationMinutes} <b>no</b> incluye el +20% de
 * {@code tempo_extra}: eso lo aplica {@code SlotGrid.applyTempoExtra} en el
 * punto de uso, porque el flag lo marca el admin sobre un turno concreto y no
 * es parte de la tarifa (ADR 011).
 */
public record ComboPricing(
        Long baseServiceId,
        BigDecimal basePrice,
        int baseDurationMinutes,
        List<Line> addons,
        BigDecimal totalPrice,
        int totalDurationMinutes
) {

    public record Line(Long serviceId, BigDecimal price, int durationMinutes) {
    }
}
