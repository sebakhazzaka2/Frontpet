package com.frontpet.booking.dto;

import com.frontpet.booking.domain.Porte;

import java.time.LocalDate;
import java.util.List;

/**
 * Parámetros de {@code GET /api/v1/availability}. {@code addonIds} vacío =
 * solo el banho base.
 */
public record AvailabilityQuery(
        Long baseServiceId,
        Porte porte,
        List<Long> addonIds,
        LocalDate data
) {
    public AvailabilityQuery {
        addonIds = addonIds == null ? List.of() : List.copyOf(addonIds);
    }
}
