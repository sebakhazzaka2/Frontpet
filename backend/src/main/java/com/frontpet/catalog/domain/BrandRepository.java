package com.frontpet.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByTenantId(UUID tenantId);

    // IgnoreCase compila a LOWER(nome) = LOWER(?), que es exactamente lo que
    // cubre el índice funcional uq_brands_tenant_nome_lower (V9).
    Optional<Brand> findByTenantIdAndNomeIgnoreCase(UUID tenantId, String nome);
}
