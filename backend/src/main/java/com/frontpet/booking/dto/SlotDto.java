package com.frontpet.booking.dto;

/**
 * Un horario de inicio candidato. {@code horario} en {@code HH:mm} local
 * ({@code America/Sao_Paulo}), no un instante UTC — el wizard lo muestra tal
 * cual sin reconvertir zona.
 *
 * <p>Se devuelven también los {@code disponivel = false} (ADR 020 §6): el
 * wizard necesita distinguir "ocupado" de "fuera de horario".
 */
public record SlotDto(
        String horario,
        boolean disponivel
) {
}
