package com.frontpet.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, Long> {

    List<BusinessHours> findByTenantIdOrderByDiaSemana(UUID tenantId);

    Optional<BusinessHours> findByTenantIdAndDiaSemana(UUID tenantId, Short diaSemana);
}
