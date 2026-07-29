package com.frontpet.orders.api;

import com.frontpet.common.PageResponse;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.orders.OrderService;
import com.frontpet.orders.domain.OrderStatus;
import com.frontpet.orders.dto.OrderDetail;
import com.frontpet.orders.dto.OrderStatusCounts;
import com.frontpet.orders.dto.OrderSummary;
import com.frontpet.orders.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestão de pedidos del admin (tareas 4.14/4.8). Protegido por
 * {@code anyRequest().authenticated()} de {@link com.frontpet.config.SecurityConfig}
 * — no necesita regla propia.
 *
 * <p>El {@code tenantId} sale de {@link AdminUser}, no de {@code CurrentTenant}
 * — mismo criterio que {@code AdminProductController}.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public PageResponse<OrderSummary> list(
            @AuthenticationPrincipal AdminUser admin,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 24, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return PageResponse.of(orderService.list(admin.getTenantId(), status, pageable));
    }

    /** Declarado antes de {@code /{publicId}} para que ese path variable no lo capture. */
    @GetMapping("/counts")
    public OrderStatusCounts counts(@AuthenticationPrincipal AdminUser admin) {
        return orderService.countByStatus(admin.getTenantId());
    }

    @GetMapping("/{publicId}")
    public OrderDetail get(@AuthenticationPrincipal AdminUser admin, @PathVariable UUID publicId) {
        return orderService.getByPublicId(admin.getTenantId(), publicId);
    }

    @PatchMapping("/{publicId}/status")
    public OrderDetail updateStatus(@AuthenticationPrincipal AdminUser admin,
                                    @PathVariable UUID publicId,
                                    @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(admin.getTenantId(), publicId, request.status());
    }
}
