package com.frontpet.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateScheduleBlockRequest(
        @NotNull LocalDate dataDesde,
        @NotNull LocalDate dataHasta,
        String motivo
) {
}
