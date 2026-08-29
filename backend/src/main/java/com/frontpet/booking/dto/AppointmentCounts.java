package com.frontpet.booking.dto;

/**
 * Contadores operacionales para o mini-dashboard do admin (CLAUDE.md §6/§7 —
 * sem métricas analíticas, só conteos). {@code hoje} e {@code proximos7Dias}
 * excluem CANCELLED; {@code aguardandoConfirmacao} é o total de PENDING na
 * janela combinada {@code [hoje, hoje+8)}.
 */
public record AppointmentCounts(
        long hoje,
        long proximos7Dias,
        long aguardandoConfirmacao
) {
}
