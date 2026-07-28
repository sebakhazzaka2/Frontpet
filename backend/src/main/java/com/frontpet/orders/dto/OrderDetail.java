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
 * @param whatsappMessage mensaje pre-formateado (ADR 010) ya armado por el
 *                        backend con el loop sobre {@code items}. El frontend
 *                        solo hace {@code encodeURIComponent} y redirige a
 *                        {@code wa.me/<numero>?text=<mensaje>} — la plantilla
 *                        vive en el backend porque acá es donde se conocen
 *                        los datos reales del pedido recién creado
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
        Instant cancelledAt,
        String whatsappMessage
) {
}
