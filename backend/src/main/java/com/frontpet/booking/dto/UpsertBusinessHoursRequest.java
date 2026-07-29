package com.frontpet.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Una fila del upsert de {@code business_hours} (tarea 5.7). {@code diaSemana}
 * en convención ISO-8601 (1=Lun..7=Dom, ADR 013 §8) — igual que la entidad.
 */
public record UpsertBusinessHoursRequest(
        @NotNull @Min(1) @Max(7) Short diaSemana,
        @NotNull Boolean activo,
        @NotNull LocalTime abertura,
        @NotNull LocalTime fechamento,
        LocalTime pausaInicio,
        LocalTime pausaFin
) {
}
