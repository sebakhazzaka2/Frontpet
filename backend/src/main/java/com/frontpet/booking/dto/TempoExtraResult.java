package com.frontpet.booking.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response de {@code PATCH /admin/appointments/{publicId}/tempo-extra}.
 *
 * <p>{@code aviso}: {@code null} si no hay solapamiento nuevo tras el
 * recálculo; si lo hay, avisa pero <b>igual persiste</b> el cambio — el
 * admin decide qué hacer con la agenda (ADR 011: avisar, no bloquear).
 */
public record TempoExtraResult(
        UUID publicId,
        Integer totalDurationMinutes,
        Instant endAt,
        String aviso
) {
}
