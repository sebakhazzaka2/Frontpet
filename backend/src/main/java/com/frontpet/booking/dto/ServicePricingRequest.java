package com.frontpet.booking.dto;

import com.frontpet.booking.domain.Porte;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ServicePricingRequest(
        @NotNull Porte size,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @Positive Integer durationMinutes
) {
}
