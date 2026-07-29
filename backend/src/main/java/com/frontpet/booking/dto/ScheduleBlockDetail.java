package com.frontpet.booking.dto;

import java.time.LocalDate;

public record ScheduleBlockDetail(Long id, LocalDate dataDesde, LocalDate dataHasta, String motivo) {
}
