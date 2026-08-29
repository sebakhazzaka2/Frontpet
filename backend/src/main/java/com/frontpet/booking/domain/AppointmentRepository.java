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
     * Listado admin de turnos, sin filtro de fecha (docs/booking-api-contracts.md,
     * {@code GET /admin/appointments?status=}). {@code status} es opcional —
     * mismo patrón {@code (:status IS NULL OR ...)} que {@code OrderRepository.findSummaries}.
     *
     * <p>Separado de {@link #findForAdminInWindow} (en vez de un único método
     * con {@code windowStart}/{@code windowEnd} también nulleables) porque
     * Postgres no puede inferir el tipo de un parámetro {@code TIMESTAMPTZ}
     * que solo se usa contra {@code NULL} en el plan de la query — "could not
     * determine data type of parameter". {@code status} no tiene ese problema
     * porque es un {@code VARCHAR} con {@code @Enumerated(STRING)}.
     */
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.tenantId = :tenantId
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.startAt
            """)
    List<Appointment> findForAdmin(
            @Param("tenantId") UUID tenantId,
            @Param("status") AppointmentStatus status
    );

    /** Igual que {@link #findForAdmin}, acotado a {@code [windowStart, windowEnd)} — filtro por {@code data}. */
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.tenantId = :tenantId
              AND (:status IS NULL OR a.status = :status)
              AND a.startAt >= :windowStart
              AND a.startAt < :windowEnd
            ORDER BY a.startAt
            """)
    List<Appointment> findForAdminInWindow(
            @Param("tenantId") UUID tenantId,
            @Param("status") AppointmentStatus status,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    /**
     * Conteo para o mini-dashboard admin ({@code AppointmentService.countsForAdmin}):
     * turnos ativos (≠ CANCELLED) que começam dentro de {@code [windowStart, windowEnd)}.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.tenantId = :tenantId
              AND a.status <> :cancelled
              AND a.startAt >= :windowStart
              AND a.startAt < :windowEnd
            """)
    long countActiveInWindow(
            @Param("tenantId") UUID tenantId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd,
            @Param("cancelled") AppointmentStatus cancelled
    );

    /** Igual que {@link #countActiveInWindow}, filtrado por um status puntual. */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.tenantId = :tenantId
              AND a.status = :status
              AND a.startAt >= :windowStart
              AND a.startAt < :windowEnd
            """)
    long countByStatusInWindow(
            @Param("tenantId") UUID tenantId,
            @Param("status") AppointmentStatus status,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

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

    /**
     * Igual que {@link #findOccupiedIntervals}, excluyendo un turno puntual —
     * lo usa {@code AppointmentServiceImpl.updateTempoExtra} para recontar
     * solapamientos contra el nuevo fin sin que el propio turno se cuente a
     * sí mismo.
     */
    @Query("""
            SELECT new com.frontpet.booking.dto.OccupiedInterval(a.startAt, a.endAt)
            FROM Appointment a
            WHERE a.tenantId = :tenantId
              AND a.id <> :excludeId
              AND a.status <> :cancelled
              AND a.startAt >= :lowerGuard
              AND a.startAt < :windowEnd
              AND a.endAt > :windowStart
            """)
    List<OccupiedInterval> findOccupiedIntervalsExcluding(
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") Long excludeId,
            @Param("lowerGuard") Instant lowerGuard,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd,
            @Param("cancelled") AppointmentStatus cancelled
    );
}
