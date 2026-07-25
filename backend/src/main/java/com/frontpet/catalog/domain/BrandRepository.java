package com.frontpet.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByTenantId(UUID tenantId);
}
