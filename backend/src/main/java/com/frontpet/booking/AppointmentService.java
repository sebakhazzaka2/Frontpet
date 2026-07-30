package com.frontpet.booking;

import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.dto.AdminAppointmentDetail;
import com.frontpet.booking.dto.AppointmentDetail;
import com.frontpet.booking.dto.CreateAppointmentRequest;
import com.frontpet.booking.dto.TempoExtraResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    /**
     * Crea un turno (tarea 5.5). Recalcula precio/duração server-side
     * (nunca confía en lo que mande el cliente, ADR 020) y controla la
     * concurrencia del último cupo con {@code pg_advisory_xact_lock}.
     *
     * @throws IllegalArgumentException se o combo é inválido, o dia não está
     *                                   disponível, ou o horário não está na
     *                                   grilla de candidatos (400)
     * @throws SlotUnavailableException se o cupo já foi consumido por outro
     *                                   turno concorrente (409)
     */
    AppointmentDetail create(UUID tenantId, CreateAppointmentRequest request);

    /** Vista pública reducida — sem telefone completo. */
    AppointmentDetail getPublicByPublicId(UUID tenantId, UUID publicId);

    /** Listado admin, ambos filtros opcionales (tarea 5.5/5.6). */
    List<AdminAppointmentDetail> listForAdmin(UUID tenantId, LocalDate data, AppointmentStatus status);

    /**
     * Transições {@code PENDING → CONFIRMED|CANCELLED}, {@code CONFIRMED → CANCELLED}.
     * No-op idempotente si ya está en ese estado (mismo patrón que
     * {@code orders.OrderServiceImpl}).
     */
    AdminAppointmentDetail updateStatus(UUID tenantId, UUID publicId, AppointmentStatus newStatus);

    /**
     * Marca/desmarca {@code tempo_extra} y recalcula la duração total (+20%,
     * nunca el precio — ADR 011). Avisa si el nuevo fin supera la capacidad,
     * pero persiste igual.
     */
    TempoExtraResult updateTempoExtra(UUID tenantId, UUID publicId, boolean tempoExtra);
}
