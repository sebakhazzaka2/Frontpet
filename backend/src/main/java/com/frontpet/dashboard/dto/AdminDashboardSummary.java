package com.frontpet.dashboard.dto;

import com.frontpet.booking.dto.AppointmentCounts;
import com.frontpet.catalog.dto.ProductStockCounts;

/**
 * Mini-dashboard operacional do admin (CLAUDE.md §6/§7, ROADMAP tarea 7.1/7.3).
 * Só conteos operacionais — nada de conversão, faturamento, ticket médio ou
 * trend pills (ADR 003/008). Composição pura de {@code OrderService},
 * {@code AppointmentService} e {@code ProductService}; este módulo não tem
 * domain próprio.
 */
public record AdminDashboardSummary(
        long pedidosPendentes,
        AppointmentCounts turnos,
        ProductStockCounts produtos
) {
}
