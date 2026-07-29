package com.frontpet.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    List<ScheduleBlock> findByTenantIdOrderByDataDesde(UUID tenantId);

    /** Existe un bloqueo activo que cubre la fecha consultada (ADR 020 §6, motivo BLOQUEADO). */
    Optional<ScheduleBlock> findFirstByTenantIdAndDataDesdeLessThanEqualAndDataHastaGreaterThanEqual(
            UUID tenantId, LocalDate data, LocalDate mismaData
    );

    Optional<ScheduleBlock> findByIdAndTenantId(Long id, UUID tenantId);
}
