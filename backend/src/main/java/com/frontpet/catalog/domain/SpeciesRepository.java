package com.frontpet.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeciesRepository extends JpaRepository<Species, Long> {

    List<Species> findByTenantId(UUID tenantId);

    Optional<Species> findByTenantIdAndSlug(UUID tenantId, String slug);
}
