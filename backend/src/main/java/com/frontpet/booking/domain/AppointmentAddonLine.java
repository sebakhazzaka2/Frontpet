package com.frontpet.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Línea de adicional de un turno (N:M vía tabla {@code appointment_addons}).
 *
 * <p>{@code @Embeddable}, no {@code @Entity}: la tabla no tiene {@code id}
 * propio ni {@code updated_at} — su PK es compuesta
 * {@code (appointment_id, service_id)}. {@code created_at} queda sin mapear
 * a propósito (lo llena el {@code DEFAULT now()} de la migración;
 * {@code ddl-auto: validate} no exige que toda columna esté mapeada).
 */
@Embeddable
@Getter @Setter @NoArgsConstructor
public class AppointmentAddonLine {

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "duration_snapshot", nullable = false)
    private Integer durationSnapshot;

    public AppointmentAddonLine(Long serviceId, BigDecimal priceSnapshot, Integer durationSnapshot) {
        this.serviceId = serviceId;
        this.priceSnapshot = priceSnapshot;
        this.durationSnapshot = durationSnapshot;
    }

    // service_id ya es único por turno (la PK compuesta lo garantiza en DB) —
    // alcanza como clave de igualdad para que Hibernate diffe la colección.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppointmentAddonLine other)) return false;
        return Objects.equals(serviceId, other.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }
}
