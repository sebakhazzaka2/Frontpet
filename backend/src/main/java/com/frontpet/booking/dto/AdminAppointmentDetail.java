package com.frontpet.booking.dto;

import com.frontpet.booking.domain.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vista admin de un turno — a diferencia de {@link AppointmentDetail} incluye
 * {@code clienteTelefone} completo (requiere cookie JWT, docs/booking-api-contracts.md
 * sección Admin).
 */
public record AdminAppointmentDetail(
        UUID publicId,
        AppointmentStatus status,
        Instant startAt,
        Instant endAt,
        String clienteNome,
        String clienteTelefone,
        String petNome,
        String baseServiceNome,
        List<String> addonsNomes,
        BigDecimal totalPriceSnapshot,
        Integer totalDurationMinutes,
        Boolean tempoExtra
) {
}
