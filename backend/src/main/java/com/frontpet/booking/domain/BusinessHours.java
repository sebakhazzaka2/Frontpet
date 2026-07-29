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

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Horário de atendimento — uma fila por (tenant, dia_semana), ISO-8601
 * (1=Seg..7=Dom, ADR 013 §8). {@code pausaInicio}/{@code pausaFin} nulos =
 * sem pausa (caso FrontPet).
 */
@Entity
@Table(name = "business_hours")
@Getter @Setter @NoArgsConstructor
public class BusinessHours extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private LocalTime abertura;

    @Column(nullable = false)
    private LocalTime fechamento;

    @Column(name = "pausa_inicio")
    private LocalTime pausaInicio;

    @Column(name = "pausa_fin")
    private LocalTime pausaFin;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BusinessHours other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
