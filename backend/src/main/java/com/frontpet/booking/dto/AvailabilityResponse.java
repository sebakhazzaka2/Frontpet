package com.frontpet.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de {@code GET /api/v1/availability} (contrato en
 * {@code docs/booking-api-contracts.md}).
 *
 * <p>{@code duracaoTotalMinutes} y {@code precoTotal} vienen siempre, incluso
 * cuando {@code indisponibilidade != null}: el combo elegido es válido aunque
 * ese día puntual no ofrezca nada, y el wizard muestra el precio mientras el
 * usuario prueba otras fechas.
 */
public record AvailabilityResponse(
        LocalDate data,
        int duracaoTotalMinutes,
        BigDecimal precoTotal,
        Indisponibilidade indisponibilidade,
        List<SlotDto> slots
) {
}
