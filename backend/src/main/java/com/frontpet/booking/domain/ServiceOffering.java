package com.frontpet.booking.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Banho base ou adicional (ADR 011). Mapeia {@code services} —
 * {@code Service} choca com {@code @org.springframework.stereotype.Service},
 * daí o nome.
 *
 * <p>Sem {@code public_id}: admin edita por {@code id} numérico, e a lista é
 * fixa em seed (admin não cria nem apaga, ADR 009). Equals/hashCode por
 * {@code id} — ao contrário de {@code Product}/{@code Order}, não há UUID
 * público que sirva de chave de negócio antes do insert.
 */
@Entity
@Table(name = "services")
@Getter @Setter @NoArgsConstructor
public class ServiceOffering extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ServiceType type;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicePricing> pricing = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceOffering other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
