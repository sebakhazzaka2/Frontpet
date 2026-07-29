package com.frontpet.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByTenantIdAndPublicId(UUID tenantId, UUID publicId);

    // La query de solapamiento (ADR 020 §3) y los listados por fecha/status
    // se agregan en el Bloque C/G, cuando AvailabilityServiceImpl y el admin
    // de turnos definan su forma exacta — no se adivina acá.
}
