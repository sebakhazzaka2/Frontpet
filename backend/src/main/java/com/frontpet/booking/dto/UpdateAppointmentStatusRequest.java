package com.frontpet.booking.dto;

import com.frontpet.booking.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
        @NotNull AppointmentStatus status
) {
}
