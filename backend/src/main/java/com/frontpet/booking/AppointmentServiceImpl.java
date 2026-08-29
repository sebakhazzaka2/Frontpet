package com.frontpet.booking;

import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentAddonLine;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ScheduleBlockRepository;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.booking.dto.AdminAppointmentDetail;
import com.frontpet.booking.dto.AppointmentAddonDetail;
import com.frontpet.booking.dto.AppointmentCounts;
import com.frontpet.booking.dto.AppointmentDetail;
import com.frontpet.booking.dto.ComboPricing;
import com.frontpet.booking.dto.CreateAppointmentRequest;
import com.frontpet.booking.dto.CreateManualAppointmentRequest;
import com.frontpet.booking.dto.ManualAppointmentResult;
import com.frontpet.booking.dto.TempoExtraResult;
import com.frontpet.common.PhoneNormalizer;
import com.frontpet.common.UuidV7;
import com.frontpet.tenant.TenantSettingsService;
import com.frontpet.tenant.dto.BookingSettings;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creación y gestión de turnos (ADR 020). {@code create} es la única
 * operación que toca concurrencia real — el resto son lecturas o
 * transiciones simples de estado.
 */
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    /** PENDING/CONFIRMED/CANCELLED, nunca se vuelve a PENDING (CLAUDE.md §6, mismo criterio que orders). */
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(Map.of(
            AppointmentStatus.PENDING, Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
            AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.CANCELLED),
            AppointmentStatus.CANCELLED, Set.of()));

    private final AvailabilityService availabilityService;
    private final TenantSettingsService tenantSettingsService;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    @Transactional
    public AppointmentDetail create(UUID tenantId, CreateAppointmentRequest request) {
        // Honeypot: mensagem genérica a propósito — mismo criterio que
        // orders.OrderServiceImpl.create (tarea 4.15).
        if (StringUtils.hasText(request.honeypot())) {
            throw new IllegalArgumentException("Requisição inválida.");
        }

        // Combo ANTES que nada: valida existencia/tipo/porte de base+adicionais
        // con el mismo cálculo que /availability (ADR 020) — nunca confía en
        // precio/duração que mande el cliente.
        ComboPricing combo = availabilityService.resolveCombo(
                tenantId, request.baseServiceId(), request.addonIds(), request.porte());

        BookingSettings settings = tenantSettingsService.booking(tenantId);
        LocalDate data = request.data();
        ZonedDateTime agora = ZonedDateTime.now(clock).withZoneSameInstant(SlotGrid.ZONE_ID);
        LocalDate hoje = agora.toLocalDate();
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(settings.anticipacaoMaxDias()))) {
            throw new IllegalArgumentException("Data fora da janela de agendamento.");
        }

        short diaSemana = (short) data.getDayOfWeek().getValue();
        BusinessHours horario = businessHoursRepository.findByTenantIdAndDiaSemana(tenantId, diaSemana)
                .filter(BusinessHours::getActivo)
                .orElseThrow(() -> new IllegalArgumentException("Este dia não está disponível para agendamento."));

        if (scheduleBlockRepository
                .findFirstByTenantIdAndDataDesdeLessThanEqualAndDataHastaGreaterThanEqual(tenantId, data, data)
                .isPresent()) {
            throw new IllegalArgumentException("Esta data está bloqueada para agendamento.");
        }

        List<LocalTime> candidatos = SlotGrid.candidateStarts(
                data,
                horario.getAbertura(),
                horario.getFechamento(),
                horario.getPausaInicio(),
                horario.getPausaFin(),
                combo.totalDurationMinutes(),
                agora,
                settings.anticipacaoMinHoras());

        if (!candidatos.contains(request.horario())) {
            throw new IllegalArgumentException("Horário inválido para este serviço nesta data.");
        }

        Instant slotStart = ZonedDateTime.of(data, request.horario(), SlotGrid.ZONE_ID).toInstant();
        Instant slotEnd = slotStart.plus(combo.totalDurationMinutes(), ChronoUnit.MINUTES);

        // Se libera solo con el commit/rollback de esta transacción — nunca
        // hay un unlock que olvidar. Granularidad tenant+día: colisiones de
        // dayKey solo serializan de más, nunca dan un resultado incorrecto
        // (ADR 020 §4).
        lockTenantDay(tenantId, data);

        Instant lowerGuard = slotStart.minus(1, ChronoUnit.DAYS);
        long solapados = appointmentRepository
                .findOccupiedIntervals(tenantId, lowerGuard, slotStart, slotEnd, AppointmentStatus.CANCELLED)
                .size();
        if (solapados >= settings.capacidadeAtendimento()) {
            throw new SlotUnavailableException("Este horário não tem mais vagas disponíveis.");
        }

        ServiceOffering baseService = serviceOfferingRepository
                .findByIdAndTenantIdAndType(request.baseServiceId(), tenantId, ServiceType.BASE)
                .orElseThrow(() -> new IllegalArgumentException("Serviço base inválido."));

        Appointment appointment = buildAppointment(
                tenantId, combo, baseService, request.porte(), slotStart, slotEnd,
                request.clienteNome(), request.clienteTelefone(), request.petNome(),
                request.petRaca(), request.observacoes());

        Appointment saved = appointmentRepository.save(appointment);
        return toDetail(saved);
    }

    /**
     * Turno manual do admin (ADR 021). Reusa {@code resolveCombo} — precio e
     * duração seguem calculados server-side, nunca confiando no request —
     * mas relaxa 3 restrições de {@link #create}, cada uma virando um aviso
     * em vez de uma exceção: grilla de candidatos (nem se calcula), dia
     * bloqueado/fora de horário e capacidade excedida.
     */
    @Override
    @Transactional
    public ManualAppointmentResult createManual(UUID tenantId, CreateManualAppointmentRequest request) {
        ComboPricing combo = availabilityService.resolveCombo(
                tenantId, request.baseServiceId(), request.addonIds(), request.porte());

        BookingSettings settings = tenantSettingsService.booking(tenantId);
        LocalDate data = request.data();
        List<String> avisos = new ArrayList<>();

        short diaSemana = (short) data.getDayOfWeek().getValue();
        boolean diaAtivo = businessHoursRepository.findByTenantIdAndDiaSemana(tenantId, diaSemana)
                .map(BusinessHours::getActivo)
                .orElse(false);
        if (!diaAtivo) {
            avisos.add("Este dia está fora do horário de funcionamento normal.");
        }

        if (scheduleBlockRepository
                .findFirstByTenantIdAndDataDesdeLessThanEqualAndDataHastaGreaterThanEqual(tenantId, data, data)
                .isPresent()) {
            avisos.add("Esta data está bloqueada para agendamento online.");
        }

        // Sem SlotGrid.candidateStarts: o turno manual não precisa cair na
        // grilha de 30 min — captura um horário já combinado por telefone.
        Instant slotStart = ZonedDateTime.of(data, request.horario(), SlotGrid.ZONE_ID).toInstant();
        Instant slotEnd = slotStart.plus(combo.totalDurationMinutes(), ChronoUnit.MINUTES);

        lockTenantDay(tenantId, data);

        Instant lowerGuard = slotStart.minus(1, ChronoUnit.DAYS);
        long solapados = appointmentRepository
                .findOccupiedIntervals(tenantId, lowerGuard, slotStart, slotEnd, AppointmentStatus.CANCELLED)
                .size();
        if (solapados >= settings.capacidadeAtendimento()) {
            avisos.add("Este horário supera a capacidade (" + settings.capacidadeAtendimento() + ").");
        }

        ServiceOffering baseService = serviceOfferingRepository
                .findByIdAndTenantIdAndType(request.baseServiceId(), tenantId, ServiceType.BASE)
                .orElseThrow(() -> new IllegalArgumentException("Serviço base inválido."));

        Appointment appointment = buildAppointment(
                tenantId, combo, baseService, request.porte(), slotStart, slotEnd,
                request.clienteNome(), request.clienteTelefone(), request.petNome(),
                request.petRaca(), request.observacoes());

        Appointment saved = appointmentRepository.save(appointment);
        AdminAppointmentDetail detail = toAdminDetails(List.of(saved)).get(0);
        return new ManualAppointmentResult(
                detail.publicId(), detail.status(), detail.startAt(), detail.endAt(),
                detail.clienteNome(), detail.clienteTelefone(), detail.petNome(),
                detail.baseServiceNome(), detail.addonsNomes(), detail.totalPriceSnapshot(),
                detail.totalDurationMinutes(), detail.tempoExtra(), avisos);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentDetail getPublicByPublicId(UUID tenantId, UUID publicId) {
        Appointment appointment = appointmentRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new AppointmentNotFoundException("Turno não encontrado: " + publicId));
        return toDetail(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAppointmentDetail> listForAdmin(
            UUID tenantId, LocalDate data, LocalDate desde, LocalDate hasta, AppointmentStatus status) {
        List<Appointment> appointments;
        if (data != null) {
            Instant windowStart = ZonedDateTime.of(data, LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();
            Instant windowEnd = ZonedDateTime.of(data.plusDays(1), LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();
            appointments = appointmentRepository.findForAdminInWindow(tenantId, status, windowStart, windowEnd);
        } else if (desde != null && hasta != null) {
            Instant windowStart = ZonedDateTime.of(desde, LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();
            Instant windowEnd = ZonedDateTime.of(hasta.plusDays(1), LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();
            appointments = appointmentRepository.findForAdminInWindow(tenantId, status, windowStart, windowEnd);
        } else if (desde != null || hasta != null) {
            throw new IllegalArgumentException("Informe desde e hasta juntos, ou nenhum dos dois.");
        } else {
            appointments = appointmentRepository.findForAdmin(tenantId, status);
        }
        return toAdminDetails(appointments);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentCounts countsForAdmin(UUID tenantId) {
        ZonedDateTime agora = ZonedDateTime.now(clock).withZoneSameInstant(SlotGrid.ZONE_ID);
        LocalDate hoje = agora.toLocalDate();
        Instant inicioHoje = ZonedDateTime.of(hoje, LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();
        Instant fimHoje = ZonedDateTime.of(hoje.plusDays(1), LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();
        Instant fimProximos7Dias = ZonedDateTime.of(hoje.plusDays(8), LocalTime.MIDNIGHT, SlotGrid.ZONE_ID).toInstant();

        long turnosHoje = appointmentRepository.countActiveInWindow(
                tenantId, inicioHoje, fimHoje, AppointmentStatus.CANCELLED);
        long turnosProximos7Dias = appointmentRepository.countActiveInWindow(
                tenantId, fimHoje, fimProximos7Dias, AppointmentStatus.CANCELLED);
        long aguardandoConfirmacao = appointmentRepository.countByStatusInWindow(
                tenantId, AppointmentStatus.PENDING, inicioHoje, fimProximos7Dias);

        return new AppointmentCounts(turnosHoje, turnosProximos7Dias, aguardandoConfirmacao);
    }

    @Override
    @Transactional
    public AdminAppointmentDetail updateStatus(UUID tenantId, UUID publicId, AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new AppointmentNotFoundException("Turno não encontrado: " + publicId));

        AppointmentStatus current = appointment.getStatus();
        if (current == newStatus) {
            // No-op idempotente: repetir la misma transición no vuelve a sellar el timestamp.
            return toAdminDetails(List.of(appointment)).get(0);
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Não é possível mudar o turno de " + current + " para " + newStatus + ".");
        }

        appointment.setStatus(newStatus);
        Instant now = clock.instant();
        if (newStatus == AppointmentStatus.CONFIRMED) {
            appointment.setConfirmedAt(now);
        } else if (newStatus == AppointmentStatus.CANCELLED) {
            appointment.setCancelledAt(now);
        }
        return toAdminDetails(List.of(appointment)).get(0);
    }

    @Override
    @Transactional
    public TempoExtraResult updateTempoExtra(UUID tenantId, UUID publicId, boolean tempoExtra) {
        Appointment appointment = appointmentRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new AppointmentNotFoundException("Turno não encontrado: " + publicId));

        // Recalcula desde el combo vigente (no desde total_duration_minutes
        // guardado, que ya puede traer un +20% aplicado de un toggle previo)
        // para que activar→desactivar→activar sea reversible sin drift.
        List<Long> addonIds = appointment.getAddons().stream().map(AppointmentAddonLine::getServiceId).toList();
        ComboPricing combo = availabilityService.resolveCombo(
                tenantId, appointment.getBaseService().getId(), addonIds, appointment.getSize());

        int newDuration = SlotGrid.applyTempoExtra(combo.totalDurationMinutes(), tempoExtra);
        Instant newEnd = appointment.getStartAt().plus(newDuration, ChronoUnit.MINUTES);

        BookingSettings settings = tenantSettingsService.booking(tenantId);
        Instant lowerGuard = appointment.getStartAt().minus(1, ChronoUnit.DAYS);
        long solapados = appointmentRepository
                .findOccupiedIntervalsExcluding(
                        tenantId, appointment.getId(), lowerGuard,
                        appointment.getStartAt(), newEnd, AppointmentStatus.CANCELLED)
                .size();

        String aviso = null;
        if (solapados >= settings.capacidadeAtendimento()) {
            aviso = "Este horário supera a capacidade (" + settings.capacidadeAtendimento()
                    + ") com este novo horário de término.";
        }

        // Avisa, mas persiste igual (ADR 011): a decisão de reorganizar a
        // agenda é do admin, não do sistema.
        appointment.setTempoExtra(tempoExtra);
        appointment.setTotalDurationMinutes(newDuration);
        appointment.setEndAt(newEnd);

        return new TempoExtraResult(appointment.getPublicId(), newDuration, newEnd, aviso);
    }

    // ---- construção da entidade --------------------------------------------

    /** Campos comuns entre {@link #create} e {@link #createManual} — a única diferença é o que valida antes. */
    private Appointment buildAppointment(
            UUID tenantId, ComboPricing combo, ServiceOffering baseService, Porte porte,
            Instant slotStart, Instant slotEnd, String clienteNome, String clienteTelefone,
            String petNome, String petRaca, String observacoes) {
        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(tenantId);
        appointment.setBaseService(baseService);
        appointment.setSize(porte);
        appointment.setStartAt(slotStart);
        appointment.setEndAt(slotEnd);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setClienteNome(clienteNome);
        appointment.setClienteTelefone(clienteTelefone);
        appointment.setClienteTelefoneNorm(PhoneNormalizer.normalizeBr(clienteTelefone));
        appointment.setPetNome(petNome);
        appointment.setPetRaca(petRaca);
        appointment.setBasePriceSnapshot(combo.basePrice());
        appointment.setTotalPriceSnapshot(combo.totalPrice());
        appointment.setTempoExtra(false);
        appointment.setTotalDurationMinutes(combo.totalDurationMinutes());
        appointment.setObservacoes(observacoes);
        for (ComboPricing.Line line : combo.addons()) {
            appointment.getAddons().add(
                    new AppointmentAddonLine(line.serviceId(), line.price(), line.durationMinutes()));
        }
        return appointment;
    }

    // ---- concurrencia -----------------------------------------------------

    private void lockTenantDay(UUID tenantId, LocalDate data) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:namespace, :dayKey)")
                .setParameter("namespace", tenantId.hashCode())
                .setParameter("dayKey", (int) data.toEpochDay())
                .getSingleResult();
    }

    // ---- mapeo entidad → DTO -----------------------------------------------

    private AppointmentDetail toDetail(Appointment appointment) {
        List<Long> addonIds = appointment.getAddons().stream().map(AppointmentAddonLine::getServiceId).toList();
        Map<Long, String> nomes = resolveNomes(addonIds);
        List<AppointmentAddonDetail> addons = appointment.getAddons().stream()
                .map(l -> new AppointmentAddonDetail(
                        nomes.getOrDefault(l.getServiceId(), "Adicional"), l.getPriceSnapshot(), l.getDurationSnapshot()))
                .toList();

        return new AppointmentDetail(
                appointment.getPublicId(),
                appointment.getStatus(),
                appointment.getBaseService().getNome(),
                addons,
                appointment.getSize(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getBasePriceSnapshot(),
                appointment.getTotalPriceSnapshot(),
                appointment.getTotalDurationMinutes(),
                appointment.getTempoExtra(),
                appointment.getClienteNome(),
                appointment.getPetNome());
    }

    /**
     * Batch a propósito: {@code listForAdmin} puede traer varias decenas de
     * turnos, y resolver el nombre de cada serviço uno por uno sería N+1
     * (mismo criterio que evitar {@code @EntityGraph} en listados paginados
     * de {@code OrderRepository}).
     */
    private List<AdminAppointmentDetail> toAdminDetails(List<Appointment> appointments) {
        Set<Long> serviceIds = new java.util.LinkedHashSet<>();
        for (Appointment a : appointments) {
            serviceIds.add(a.getBaseService().getId());
            a.getAddons().forEach(l -> serviceIds.add(l.getServiceId()));
        }
        Map<Long, String> nomes = resolveNomes(new ArrayList<>(serviceIds));

        return appointments.stream()
                .map(a -> new AdminAppointmentDetail(
                        a.getPublicId(),
                        a.getStatus(),
                        a.getStartAt(),
                        a.getEndAt(),
                        a.getClienteNome(),
                        a.getClienteTelefone(),
                        a.getPetNome(),
                        nomes.getOrDefault(a.getBaseService().getId(), "Serviço"),
                        a.getAddons().stream().map(l -> nomes.getOrDefault(l.getServiceId(), "Adicional")).toList(),
                        a.getTotalPriceSnapshot(),
                        a.getTotalDurationMinutes(),
                        a.getTempoExtra()))
                .toList();
    }

    private Map<Long, String> resolveNomes(List<Long> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return serviceOfferingRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(ServiceOffering::getId, ServiceOffering::getNome));
    }
}
