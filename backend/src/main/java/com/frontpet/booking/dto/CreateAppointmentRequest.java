package com.frontpet.booking.dto;

import com.frontpet.booking.domain.Porte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Body de {@code POST /api/v1/appointments} — reserva pública y anônima
 * (tarea 5.5, docs/booking-api-contracts.md).
 *
 * <p>Nunca lleva precio ni duração: {@code AppointmentServiceImpl} recalcula
 * ambos server-side con {@link com.frontpet.booking.AvailabilityService#resolveCombo}
 * (ADR 020) — jamás confía en lo que mande el cliente.
 *
 * <p>{@code honeypot}: mismo patrón anti-bot de {@code CreateOrderRequest}
 * (tarea 4.15). Un formulário real nunca o preenche.
 */
public record CreateAppointmentRequest(
        @NotNull Long baseServiceId,
        List<Long> addonIds,
        @NotNull Porte porte,
        @NotNull LocalDate data,
        @NotNull LocalTime horario,
        @NotBlank String clienteNome,
        @NotBlank String clienteTelefone,
        @NotBlank String petNome,
        String petRaca,
        String observacoes,
        String honeypot
) {
    public CreateAppointmentRequest {
        addonIds = addonIds == null ? List.of() : List.copyOf(addonIds);
    }
}
