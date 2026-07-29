package com.frontpet.orders.dto;

import com.frontpet.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Body de {@code PATCH /api/v1/admin/orders/{publicId}/status}. */
public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}
