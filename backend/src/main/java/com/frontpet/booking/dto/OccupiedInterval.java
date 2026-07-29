package com.frontpet.booking.dto;

import java.time.Instant;

/**
 * Intervalo ocupado por un turno vigente ({@code PENDING} o {@code CONFIRMED},
 * ADR 013 §9). Se traen los intervalos crudos y el conteo contra la capacidad
 * se hace en memoria (ADR 020, paso 8) — una query por slot candidato sería
 * proporcional al tamaño de la grilla.
 */
public record OccupiedInterval(
        Instant startAt,
        Instant endAt
) {
    /** Solapamiento semiabierto, mismo criterio que la query (ADR 020 §3). */
    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return startAt.isBefore(otherEnd) && endAt.isAfter(otherStart);
    }
}
