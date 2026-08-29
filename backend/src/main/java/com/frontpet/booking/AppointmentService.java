package com.frontpet.booking;

import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.dto.AdminAppointmentDetail;
import com.frontpet.booking.dto.AppointmentCounts;
import com.frontpet.booking.dto.AppointmentDetail;
import com.frontpet.booking.dto.CreateAppointmentRequest;
import com.frontpet.booking.dto.CreateManualAppointmentRequest;
import com.frontpet.booking.dto.ManualAppointmentResult;
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

    /**
     * Turno manual do admin (ADR 021, Bloque A do Sprint 6). Reusa o mesmo
     * {@code resolveCombo} de {@link #create}, mas relaxa 3 restrições —
     * grilla de candidatos, capacidade e dia bloqueado/fora de horário —
     * avisando em vez de bloquear. Nunca lança {@link SlotUnavailableException}:
     * o único jeito de falhar é combo/serviço inválido (400).
     */
    ManualAppointmentResult createManual(UUID tenantId, CreateManualAppointmentRequest request);

    /** Vista pública reducida — sem telefone completo. */
    AppointmentDetail getPublicByPublicId(UUID tenantId, UUID publicId);

    /**
     * Listado admin. {@code data} (día único), {@code desde}/{@code hasta}
     * (rango) e {@code status} são todos opcionais; {@code data} tem
     * prioridade se vier junto com {@code desde}/{@code hasta} (tarea 5.5/5.6,
     * Bloque A do Sprint 6).
     */
    List<AdminAppointmentDetail> listForAdmin(
            UUID tenantId, LocalDate data, LocalDate desde, LocalDate hasta, AppointmentStatus status);

    /**
     * Contadores para o mini-dashboard admin (CLAUDE.md §7): turnos de hoje,
     * dos próximos 7 dias (excluindo hoje) e quantos deles ainda aguardam
     * confirmação (PENDING) dentro dessa janela combinada.
     */
    AppointmentCounts countsForAdmin(UUID tenantId);

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

    /**
     * Direito de eliminação LGPD (ADR 023) — {@code com.frontpet.privacy}
     * consome isto, nunca {@code AppointmentRepository} direto (CLAUDE.md §5).
     *
     * @param clienteTelefoneNorm já normalizado (ver {@code PhoneNormalizer})
     */
    List<UUID> findPublicIdsByPhone(UUID tenantId, String clienteTelefoneNorm);

    /**
     * Anonimiza (não apaga) todo turno do tenant com este telefone: dados
     * pessoais (incluindo do pet) viram marcadores, snapshot de preço/duração
     * fica intacto (ADR 023). Idempotente, mesmo critério de
     * {@code orders.OrderService#anonymizeByPhone}.
     *
     * @return quantos turnos foram anonimizados
     */
    int anonymizeByPhone(UUID tenantId, String clienteTelefoneNorm);
}
