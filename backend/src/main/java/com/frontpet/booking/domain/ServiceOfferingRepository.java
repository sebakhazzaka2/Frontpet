package com.frontpet.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<ServiceOffering> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<ServiceOffering> findByIdAndTenantIdAndType(Long id, UUID tenantId, ServiceType type);
}
