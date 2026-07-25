package com.frontpet.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Catálogo público: todos los productos activos del tenant
    List<Product> findByTenantIdAndActiveTrue(UUID tenantId);

    // Catálogo filtrado por categoría
    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN p.categories c
        WHERE p.tenantId = :tenantId
          AND p.active = true
          AND c.slug = :categorySlug
        """)
    List<Product> findByTenantIdAndCategorySlug(
        @Param("tenantId") UUID tenantId,
        @Param("categorySlug") String categorySlug
    );

    // Sección "Descuentos": productos con price_original seteado
    List<Product> findByTenantIdAndActiveTrueAndPriceOriginalIsNotNull(UUID tenantId);

    // Detalle de producto: /produtos/{slug}. Es la query del detalle público.
    Optional<Product> findByTenantIdAndSlug(UUID tenantId, String slug);

    // Chequeo de colisión al generar el slug (racao-golden, racao-golden-2, ...)
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    // Detalle por public_id — identificador interno estable, no va en URLs
    Optional<Product> findByTenantIdAndPublicId(UUID tenantId, UUID publicId);

    // Admin: buscar por nombre (búsqueda parcial)
    List<Product> findByTenantIdAndNomeContainingIgnoreCase(UUID tenantId, String nome);
}
