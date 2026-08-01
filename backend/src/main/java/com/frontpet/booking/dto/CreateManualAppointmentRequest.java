package com.frontpet.booking.dto;

import com.frontpet.booking.domain.Porte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Body de {@code POST /api/v1/admin/appointments} — turno manual do admin
 * (ADR 021, Bloque A do Sprint 6). Mesmos campos de {@link CreateAppointmentRequest}
 * exceto {@code honeypot}: não aplica atrás do login (ADR 021, tabela de relaxações).
 *
 * <p>Nunca leva preço nem duração — {@code AppointmentServiceImpl#createManual}
 * recalcula ambos server-side com o mesmo {@code resolveCombo} do turno público.
 */
public record CreateManualAppointmentRequest(
        @NotNull Long baseServiceId,
        List<Long> addonIds,
        @NotNull Porte porte,
        @NotNull LocalDate data,
        @NotNull LocalTime horario,
        @NotBlank String clienteNome,
        @NotBlank String clienteTelefone,
        @NotBlank String petNome,
        String petRaca,
        String observacoes
) {
    public CreateManualAppointmentRequest {
        addonIds = addonIds == null ? List.of() : List.copyOf(addonIds);
    }
}
