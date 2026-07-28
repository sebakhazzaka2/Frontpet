package com.frontpet.orders.dto;

/**
 * Contadores para las tabs "Todos / Pendentes / Confirmados / Cancelados" del
 * admin de pedidos (Bloque F). Fuera del contrato del Bloque 0: se agrega acá
 * porque backend es quien tiene la fuente de verdad de los conteos.
 */
public record OrderStatusCounts(
        long todos,
        long pendentes,
        long confirmados,
        long cancelados
) {
}
