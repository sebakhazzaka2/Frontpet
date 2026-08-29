package com.frontpet.dashboard.api;

import com.frontpet.booking.AppointmentService;
import com.frontpet.catalog.ProductService;
import com.frontpet.dashboard.dto.AdminDashboardSummary;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.orders.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mini-dashboard operacional do admin (ROADMAP tarea 7.1). Protegido por
 * {@code anyRequest().authenticated()} de {@code SecurityConfig} — mismo
 * criterio que os demais controllers admin.
 *
 * <p>Não tem domain nem service próprios: só compõe {@code OrderService},
 * {@code AppointmentService} e {@code ProductService} — cada um consumido
 * pela sua interfaz pública, nunca pelo repositório (CLAUDE.md §5).
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final AppointmentService appointmentService;
    private final ProductService productService;

    @GetMapping
    public AdminDashboardSummary summary(@AuthenticationPrincipal AdminUser admin) {
        long pedidosPendentes = orderService.countByStatus(admin.getTenantId()).pendentes();
        return new AdminDashboardSummary(
                pedidosPendentes,
                appointmentService.countsForAdmin(admin.getTenantId()),
                productService.countStockSummary(admin.getTenantId()));
    }
}
