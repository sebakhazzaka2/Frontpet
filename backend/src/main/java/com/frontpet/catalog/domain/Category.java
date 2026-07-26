package com.frontpet.catalog.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Category extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 80)
    private String slug;

    // Product las guarda en un Set, así que Java necesita saber cuándo dos
    // Category son "la misma". Se compara por (tenantId, slug) — la clave de
    // negocio, que es además el UNIQUE de la tabla — y no por id, porque el id
    // es null hasta que Hibernate persiste la fila.
    //
    // Ojo con los getters: si `o` es un proxy lazy, leerle el campo directo
    // devuelve null. El getter fuerza la carga.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return tenantId != null && slug != null
                && tenantId.equals(other.getTenantId())
                && slug.equals(other.getSlug());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, slug);
    }
}
