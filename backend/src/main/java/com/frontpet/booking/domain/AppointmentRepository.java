package com.frontpet.booking.domain;

import com.frontpet.booking.dto.OccupiedInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByTenantIdAndPublicId(UUID tenantId, UUID publicId);

    /**
     * Turnos vigentes que solapan la ventana consultada — el corazón del
     * cálculo de disponibilidad (ADR 020 §3).
     *
     * <p>El test de solapamiento es el de intervalos <b>semiabiertos</b>
     * ({@code startAt < fin AND endAt > inicio}): un turno que termina justo
     * cuando empieza la ventana no la ocupa. Sale directo de tener
     * {@code end_at} persistido (ADR 013 §10) — reemplaza el hack
     * {@code inicio.minusHours(3)} del repo consultorio, que asumía que
     * ningún turno dura más de 3 horas.
     *
     * <p>{@code CANCELLED} no ocupa cupo (ADR 013 §9); el índice parcial
     * {@code idx_appointments_tenant_start} codifica esa misma regla.
     *
     * <p>⚠️ {@code lowerGuard} no aporta corrección — {@code startAt <
     * windowEnd} ya alcanzaría. Aporta <b>plan de ejecución</b>: sin una cota
     * inferior sobre {@code start_at}, el índice parcial degenera en escanear
     * toda la historia del tenant en vez del rango relevante.
     */
    @Query("""
            SELECT new com.frontpet.booking.dto.OccupiedInterval(a.startAt, a.endAt)
            FROM Appointment a
            WHERE a.tenantId = :tenantId
              AND a.status <> :cancelled
              AND a.startAt >= :lowerGuard
              AND a.startAt < :windowEnd
              AND a.endAt > :windowStart
            """)
    List<OccupiedInterval> findOccupiedIntervals(
            @Param("tenantId") UUID tenantId,
            @Param("lowerGuard") Instant lowerGuard,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd,
            @Param("cancelled") AppointmentStatus cancelled
    );
}
