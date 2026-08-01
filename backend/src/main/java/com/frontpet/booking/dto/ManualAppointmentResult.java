package com.frontpet.booking.dto;

import com.frontpet.booking.domain.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response de {@code POST /api/v1/admin/appointments} — mesmo shape de
 * {@link AdminAppointmentDetail} mais {@code avisos} (ADR 021).
 *
 * <p>{@code List<String>} e não {@code String} único (como {@link TempoExtraResult#aviso()}):
 * um turno manual pode disparar vários avisos ao mesmo tempo (fora da grilla +
 * supera capacidade + dia bloqueado), diferente do toggle de tempo extra que só
 * pode gerar um tipo de sobreposição.
 */
public record ManualAppointmentResult(
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
        Boolean tempoExtra,
        List<String> avisos
) {
}
