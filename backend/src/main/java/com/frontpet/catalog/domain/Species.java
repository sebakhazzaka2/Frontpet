package com.frontpet.catalog.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "species")
@Getter @Setter @NoArgsConstructor
public class Species extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 80)
    private String slug;

    // Mismo motivo que en Category: Product las guarda en un Set.
    // Clave de negocio (tenantId, slug), getters para sobrevivir a los proxies.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Species other)) return false;
        return tenantId != null && slug != null
                && tenantId.equals(other.getTenantId())
                && slug.equals(other.getSlug());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, slug);
    }
}
