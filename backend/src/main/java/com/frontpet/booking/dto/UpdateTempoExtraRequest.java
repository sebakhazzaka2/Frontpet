package com.frontpet.booking.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTempoExtraRequest(
        @NotNull Boolean tempoExtra
) {
}
