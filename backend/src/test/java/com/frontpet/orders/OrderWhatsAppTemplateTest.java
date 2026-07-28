package com.frontpet.orders;

import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.FreteMode;
import com.frontpet.orders.domain.Order;
import com.frontpet.orders.domain.OrderStatus;
import com.frontpet.orders.dto.OrderItemDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderWhatsAppTemplateTest {

    private static final UUID PUBLIC_ID = UUID.fromString("00000000-0000-7000-8000-0000abcd1234");

    @Test
    void pendingMessageIncludesCodePriceAndAllLines() {
        Order order = pendingOrder();
        List<OrderItemDetail> items = List.of(
                new OrderItemDetail(UUID.randomUUID(), null, "Ração Golden 15kg",
                        new BigDecimal("45.90"), 1),
                new OrderItemDetail(UUID.randomUUID(), 2L, "Brinquedo Mordedor",
                        new BigDecimal("29.90"), 2));

        String message = OrderWhatsAppTemplate.build(order, items);

        assertThat(message).contains("#ABCD1234");
        assertThat(message).contains("R$ 45,90");
        assertThat(message).contains("Ração Golden 15kg");
        assertThat(message).contains("Brinquedo Mordedor");
        assertThat(message).contains(order.getClienteNome());
    }

    @Test
    void confirmedAndCancelledMessagesDiffer() {
        Order confirmed = pendingOrder();
        confirmed.setStatus(OrderStatus.CONFIRMED);
        Order cancelled = pendingOrder();
        cancelled.setStatus(OrderStatus.CANCELLED);

        String confirmedMessage = OrderWhatsAppTemplate.build(confirmed, List.of());
        String cancelledMessage = OrderWhatsAppTemplate.build(cancelled, List.of());

        assertThat(confirmedMessage).contains("confirmado").contains("#ABCD1234");
        assertThat(cancelledMessage).contains("cancelado").contains("#ABCD1234");
        assertThat(confirmedMessage).isNotEqualTo(cancelledMessage);
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setPublicId(PUBLIC_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setClienteNome("Maria Souza");
        order.setClienteTelefone("(51) 99999-8888");
        order.setFormaPagamento(FormaPagamento.PIX);
        order.setFreteMode(FreteMode.GRATIS);
        order.setEnderecoEntrega("Rua das Flores, 123");
        order.setSubtotalSnapshot(new BigDecimal("105.70"));
        return order;
    }
}
