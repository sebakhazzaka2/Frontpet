package com.frontpet.booking.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Bloqueo de rango de días (feriados, vacaciones, y — clave en ADR 012 —
 * cupos ocupados por otros canales que el admin bloquea a mano). Por día
 * completo, no por rango horario (limitación documentada en ADR 020).
 */
@Entity
@Table(name = "schedule_blocks")
@Getter @Setter @NoArgsConstructor
public class ScheduleBlock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "data_desde", nullable = false)
    private LocalDate dataDesde;

    @Column(name = "data_hasta", nullable = false)
    private LocalDate dataHasta;

    @Column(length = 200)
    private String motivo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleBlock other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
