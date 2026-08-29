package com.frontpet.catalog.dto;

/**
 * Contadores operacionais para o mini-dashboard do admin (CLAUDE.md §6/§7 —
 * sem métricas analíticas). {@code semEstoque} conta produtos ativos com
 * estoque efetivo zero: produto simples com {@code stock=0}, ou produto com
 * variantes sem nenhuma variante ativa com {@code stock>0}.
 */
public record ProductStockCounts(
        long ativos,
        long semEstoque
) {
}
