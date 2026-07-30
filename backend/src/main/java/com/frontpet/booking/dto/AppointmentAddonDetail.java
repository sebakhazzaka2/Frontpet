package com.frontpet.booking.dto;

import java.math.BigDecimal;

public record AppointmentAddonDetail(
        String nome,
        BigDecimal priceSnapshot,
        Integer durationSnapshot
) {
}
