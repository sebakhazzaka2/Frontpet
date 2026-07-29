package com.frontpet.booking.dto;

import java.time.LocalTime;

public record BusinessHoursDetail(
        Short diaSemana,
        Boolean activo,
        LocalTime abertura,
        LocalTime fechamento,
        LocalTime pausaInicio,
        LocalTime pausaFin
) {
}
