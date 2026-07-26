package com.frontpet.catalog.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID v7 generado por el service (ver common/UuidV7). Identificador
    // estable e interno: la URL pública usa el slug, no esto.
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    // Identificador legible de la URL: /produtos/racao-golden-15kg
    // Se genera desde `nome` al crear y el admin puede corregirlo. Único por tenant.
    @Column(nullable = false, length = 180)
    private String slug;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "main_image_url", columnDefinition = "TEXT")
    private String mainImageUrl;

    // NULL cuando el producto tiene variantes (el precio vive en ProductVariant)
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // NULL = sin promoción. Cuando está presente, price es el precio de oferta
    // y price_original es el precio tachado. Siempre price_original > price.
    @Column(name = "price_original", precision = 10, scale = 2)
    private BigDecimal priceOriginal;

    // Siempre presente (la columna es NOT NULL DEFAULT 0). Cuando el producto
    // tiene variantes este campo se ignora: el stock que vale es el de cada
    // ProductVariant. No es NULL como sí lo es `price` — ojo con la asimetría.
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "product_species",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "species_id")
    )
    private Set<Species> species = new HashSet<>();

    public boolean hasVariants() {
        return !variants.isEmpty();
    }

    public boolean isOnSale() {
        return priceOriginal != null;
    }

    // Clave de negocio: publicId, que el service asigna ANTES de persistir.
    // Por eso funciona incluso con la entidad todavía sin guardar, cosa que el
    // id autoincremental no permite (es null hasta el INSERT).
    //
    // Brand no lleva equals/hashCode a propósito: no vive dentro de ningún
    // Set ni List, es un @ManyToOne. Se agrega si algún día hace falta.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return publicId != null && publicId.equals(other.getPublicId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
