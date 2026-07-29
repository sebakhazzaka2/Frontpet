package com.frontpet.booking.dto;

/**
 * Por qué un día no ofrece ningún horario (ADR 020 §6). El wizard necesita
 * distinguir "cerrado" de "bloqueado" de "fuera de la ventana" — una lista
 * vacía sin motivo es ambigua para el copy.
 */
public enum Indisponibilidade {

    /** {@code business_hours.activo = false} para ese día (ej. domingo). */
    DIA_INATIVO,

    /** La fecha cae dentro de un {@code schedule_blocks} (feriado, bloqueo manual). */
    BLOQUEADO,

    /** Fecha en el pasado, o más allá de {@code anticipacao_max_dias}. */
    FORA_DA_JANELA
}
