package com.frontpet.orders.dto;

import com.frontpet.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pedido tal como aparece en la lista del admin (cards de "Gestão de
 * Pedidos"). {@code itemCount} evita traer los items completos solo para
 * mostrar "3 itens" en la card — el detalle completo se pide aparte
 * ({@link OrderDetail}) al abrir un pedido puntual.
 */
public record OrderSummary(
        UUID publicId,
        OrderStatus status,
        String clienteNome,
        String clienteTelefone,
        int itemCount,
        BigDecimal subtotal,
        Instant createdAt
) {
}
