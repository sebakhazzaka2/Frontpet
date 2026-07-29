package com.frontpet.booking.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Turno reservado (ADR 011, 012, 020). {@code publicId} lo asigna el service
 * con {@link com.frontpet.common.UuidV7} antes de persistir, igual que
 * {@code Product}/{@code Order}.
 *
 * <p>{@code startAt}/{@code endAt} son instantes absolutos (TIMESTAMPTZ) — la
 * conversión a/desde hora local ({@code America/Sao_Paulo}, ADR 020) es
 * responsabilidad de la capa de servicio, no de la entidad.
 */
@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor
public class Appointment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_service_id", nullable = false)
    private ServiceOffering baseService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Porte size;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "cliente_nome", nullable = false, length = 160)
    private String clienteNome;

    @Column(name = "cliente_telefone", nullable = false, length = 30)
    private String clienteTelefone;

    @Column(name = "cliente_telefone_norm", nullable = false, length = 20)
    private String clienteTelefoneNorm;

    @Column(name = "pet_nome", nullable = false, length = 80)
    private String petNome;

    @Column(name = "pet_raca", length = 80)
    private String petRaca;

    @Column(name = "base_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePriceSnapshot;

    @Column(name = "total_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPriceSnapshot;

    // Lo marca el admin, nunca el cliente público (ADR 011, act. 2026-07-25).
    @Column(name = "tempo_extra", nullable = false)
    private Boolean tempoExtra = false;

    // Ya incluye el +20% de tempoExtra si aplica; se recalcula una sola vez
    // al crear/confirmar (ADR 011, ADR 020 §2 — no se alinea a la grilla).
    @Column(name = "total_duration_minutes", nullable = false)
    private Integer totalDurationMinutes;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    // Columnas propias, no aportadas por Auditable: created_at/updated_at
    // marcan la fila en sí, confirmed_at/cancelled_at marcan la transición
    // de status (mismo criterio que Order).
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ElementCollection
    @CollectionTable(name = "appointment_addons", joinColumns = @JoinColumn(name = "appointment_id"))
    private List<AppointmentAddonLine> addons = new ArrayList<>();

    // Clave de negocio: publicId, asignado por el service ANTES del INSERT
    // (igual razón que Product.equals / Order.equals).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment other)) return false;
        return publicId != null && publicId.equals(other.getPublicId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
