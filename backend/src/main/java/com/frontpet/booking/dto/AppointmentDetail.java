package com.frontpet.booking.dto;

import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.Porte;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response de {@code POST /api/v1/appointments} (201) y de la vista pública
 * reducida {@code GET /api/v1/appointments/{publicId}} — mismo shape en
 * ambos, sin telefone completo (docs/booking-api-contracts.md).
 */
public record AppointmentDetail(
        UUID publicId,
        AppointmentStatus status,
        String baseServiceNome,
        List<AppointmentAddonDetail> addons,
        Porte porte,
        Instant startAt,
        Instant endAt,
        BigDecimal basePriceSnapshot,
        BigDecimal totalPriceSnapshot,
        Integer totalDurationMinutes,
        Boolean tempoExtra,
        String clienteNome,
        String petNome
) {
}
