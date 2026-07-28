package com.frontpet.orders;

import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.Order;
import com.frontpet.orders.domain.OrderStatus;
import com.frontpet.orders.dto.OrderItemDetail;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Arma el texto que sale en {@code OrderDetail.whatsappMessage} (ADR 010).
 *
 * <p>Vive en el backend, no en {@code lib/whatsapp/templates.ts} del
 * frontend: el mensaje se construye con la fila ya persistida — snapshots de
 * precio congelados y el código de pedido — así que es imposible que el
 * texto que recibe el petshop difiera de lo que quedó en la DB. El frontend
 * solo hace {@code encodeURIComponent} y arma el link con {@code wa.me} (ya
 * tiene {@code buildWhatsAppLink()} en {@code lib/data/site.ts}); el número
 * del petshop no se duplica acá.
 *
 * <p>Variantes por status (indexadas por {@link OrderStatus}, no por el
 * derogado {@code PENDING_WHATSAPP} — ADR 010 drift #4): {@code PENDING} es
 * el pedido nuevo que el cliente manda al petshop; {@code CONFIRMED} y
 * {@code CANCELLED} son los avisos que el petshop reenvía al cliente desde
 * el botón del admin (Bloque F).
 */
public final class OrderWhatsAppTemplate {

    /** Java formatea moneda pt-BR con NBSP entre "R$" y el número; se
     *  normaliza a espacio común porque esto va en texto plano de WhatsApp,
     *  no en UI — visualmente idéntico, pero un NBSP rompe comparaciones de
     *  texto (y algunos clientes de WhatsApp lo muestran raro). */
    private static final char NBSP = ' ';

    private OrderWhatsAppTemplate() {
        // clase de utilidad, no se instancia
    }

    public static String build(Order order, List<OrderItemDetail> items) {
        String codigo = orderCode(order.getPublicId().toString());
        return switch (order.getStatus()) {
            case PENDING -> buildPendingMessage(order, items, codigo);
            case CONFIRMED -> "Seu pedido " + codigo + " foi *confirmado*! Em breve entraremos em contato "
                    + "para combinar a entrega/retirada.";
            case CANCELLED -> "Seu pedido " + codigo + " foi *cancelado*. Qualquer dúvida, é só chamar por aqui.";
        };
    }

    private static String buildPendingMessage(Order order, List<OrderItemDetail> items, String codigo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá! Gostaria de fazer o pedido ").append(codigo).append(":\n\n");
        for (OrderItemDetail item : items) {
            sb.append("• ").append(item.quantidade()).append("x ").append(item.nomeSnapshot())
                    .append(" — ").append(formatPrice(item.subtotal())).append('\n');
        }
        sb.append("\n*Total: ").append(formatPrice(order.getSubtotalSnapshot())).append("*\n\n");
        sb.append("Nome: ").append(order.getClienteNome()).append('\n');
        sb.append("Telefone: ").append(order.getClienteTelefone()).append('\n');
        sb.append("Endereço: ").append(order.getEnderecoEntrega()).append('\n');
        sb.append("Pagamento: ").append(formaPagamentoLabel(order.getFormaPagamento())).append('\n');
        if (order.getHorarioEntrega() != null && !order.getHorarioEntrega().isBlank()) {
            sb.append("Horário: ").append(order.getHorarioEntrega()).append('\n');
        }
        return sb.toString();
    }

    /** Últimos 8 hex del publicId en mayúsculas: #A1B2C3D4. */
    private static String orderCode(String publicId) {
        String hex = publicId.replace("-", "");
        return "#" + hex.substring(hex.length() - 8).toUpperCase(Locale.ROOT);
    }

    private static String formatPrice(BigDecimal value) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        return currency.format(value).replace(NBSP, ' ');
    }

    private static String formaPagamentoLabel(FormaPagamento formaPagamento) {
        return switch (formaPagamento) {
            case DINHEIRO -> "Dinheiro";
            case PIX -> "PIX";
            case CARTAO_DEBITO -> "Cartão de débito";
            case CARTAO_CREDITO -> "Cartão de crédito";
        };
    }
}
