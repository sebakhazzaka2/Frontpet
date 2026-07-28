package com.frontpet.orders.dto;

import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.FreteMode;
import com.frontpet.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response de {@code POST /api/v1/orders} y de la vista de detalle del admin.
 *
 * <p>Sin {@code whatsappMessage}: el Bloque 0 lo había puesto acá asumiendo
 * que el backend arma la plantilla, pero el plan de Sprint 4 (issue #30,
 * tarea 4.9) asigna esa responsabilidad al frontend —
 * {@code buildOrderMessage()} en {@code lib/whatsapp/templates.ts}, con los
 * datos que ya devuelve este mismo record. Corregido acá antes de que el
 * Bloque C (backend de orders) lo implementara duplicado. ADR 010 ya
 * contemplaba la ambigüedad ("ubicación propuesta: /lib/whatsapp/templates.ts
 * (o equivalente backend)") — el plan la resuelve a favor del frontend.
 */
public record OrderDetail(
        UUID publicId,
        OrderStatus status,
        String clienteNome,
        String clienteTelefone,
        FormaPagamento formaPagamento,
        FreteMode freteMode,
        String enderecoEntrega,
        String horarioEntrega,
        BigDecimal subtotal,
        List<OrderItemDetail> items,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt
) {
}
