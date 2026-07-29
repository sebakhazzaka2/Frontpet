package com.frontpet.booking.dto;

import com.frontpet.booking.domain.Porte;

import java.math.BigDecimal;

public record ServicePricingDetail(Porte size, BigDecimal price, Integer durationMinutes) {
}
