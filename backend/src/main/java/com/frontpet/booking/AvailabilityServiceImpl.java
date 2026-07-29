package com.frontpet.booking;

import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ScheduleBlockRepository;
import com.frontpet.booking.domain.ServicePricingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.booking.dto.AvailabilityQuery;
import com.frontpet.booking.dto.AvailabilityResponse;
import com.frontpet.booking.dto.ComboPricing;
import com.frontpet.booking.dto.Indisponibilidade;
import com.frontpet.booking.dto.OccupiedInterval;
import com.frontpet.booking.dto.ServicePriceLine;
import com.frontpet.booking.dto.SlotDto;
import com.frontpet.tenant.TenantSettingsService;
import com.frontpet.tenant.dto.BookingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cálculo dinámico de disponibilidad (ADR 005 / ADR 020). No materializa
 * slots: los recalcula en cada consulta desde horario, bloqueos, tarifas y
 * turnos vigentes.
 *
 * <p>Resuelve en 5 queries, ninguna proporcional al tamaño de la grilla: los
 * candidatos se generan en memoria ({@link SlotGrid}) y el conteo contra la
 * capacidad se hace sobre una única lista de intervalos ocupados.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final DateTimeFormatter HORARIO = DateTimeFormatter.ofPattern("HH:mm");

    private final TenantSettingsService tenantSettingsService;
    private final ServicePricingRepository servicePricingRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID tenantId, AvailabilityQuery query) {
        // 1. Config del tenant: capacidad y ventana de anticipación.
        BookingSettings settings = tenantSettingsService.booking(tenantId);

        // 2. Combo ANTES de cualquier corte por fecha: el precio y la duración
        //    viajan en la respuesta incluso cuando el día no ofrece nada, para
        //    que el wizard los muestre mientras el usuario prueba otras fechas.
        //    Un servicio inválido es 400 sea cual sea la fecha pedida.
        ComboPricing combo = resolveCombo(tenantId, query.baseServiceId(), query.addonIds(), query.porte());
        LocalDate data = query.data();

        // 3. Ventana de anticipación.
        LocalDate hoje = ZonedDateTime.now(clock).withZoneSameInstant(SlotGrid.ZONE_ID).toLocalDate();
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(settings.anticipacaoMaxDias()))) {
            return unavailable(data, combo, Indisponibilidade.FORA_DA_JANELA);
        }

        // 4. Horario del día. dia_semana en ISO-8601 directo desde java.time,
        //    sin el mapeo %7 del repo consultorio (ADR 013 §8).
        short diaSemana = (short) data.getDayOfWeek().getValue();
        Optional<BusinessHours> hours = businessHoursRepository.findByTenantIdAndDiaSemana(tenantId, diaSemana);
        if (hours.isEmpty() || !hours.get().getActivo()) {
            return unavailable(data, combo, Indisponibilidade.DIA_INATIVO);
        }
        BusinessHours horario = hours.get();

        // 5. Bloqueos (feriados, y los cupos que el admin bloquea a mano por
        //    lo que entra por otros canales — ADR 012).
        if (scheduleBlockRepository
                .findFirstByTenantIdAndDataDesdeLessThanEqualAndDataHastaGreaterThanEqual(tenantId, data, data)
                .isPresent()) {
            return unavailable(data, combo, Indisponibilidade.BLOQUEADO);
        }

        // 6-7. Candidatos en memoria, ya filtrados por cierre, pausa y
        //      anticipación mínima.
        ZonedDateTime agora = ZonedDateTime.now(clock).withZoneSameInstant(SlotGrid.ZONE_ID);
        List<LocalTime> candidatos = SlotGrid.candidateStarts(
                data,
                horario.getAbertura(),
                horario.getFechamento(),
                horario.getPausaInicio(),
                horario.getPausaFin(),
                combo.totalDurationMinutes(),
                agora,
                settings.anticipacaoMinHoras());

        if (candidatos.isEmpty()) {
            // Día laborable y sin bloquear, pero sin ningún inicio viable: el
            // servicio no entra antes del cierre, o ya pasó la hora de todos
            // los slots de hoy. No es ninguno de los 3 motivos del ADR 020
            // §6 — se devuelve la grilla vacía sin motivo.
            return new AvailabilityResponse(data, combo.totalDurationMinutes(), combo.totalPrice(), null, List.of());
        }

        // 8. Una query de ocupación para todo el día + conteo en memoria.
        Instant windowStart = ZonedDateTime.of(data, horario.getAbertura(), SlotGrid.ZONE_ID).toInstant();
        Instant windowEnd = ZonedDateTime.of(data, horario.getFechamento(), SlotGrid.ZONE_ID).toInstant();
        List<OccupiedInterval> ocupados = appointmentRepository.findOccupiedIntervals(
                tenantId,
                windowStart.minus(1, ChronoUnit.DAYS),
                windowStart,
                windowEnd,
                AppointmentStatus.CANCELLED);

        List<SlotDto> slots = new ArrayList<>(candidatos.size());
        for (LocalTime inicio : candidatos) {
            Instant slotStart = ZonedDateTime.of(data, inicio, SlotGrid.ZONE_ID).toInstant();
            Instant slotEnd = slotStart.plus(combo.totalDurationMinutes(), ChronoUnit.MINUTES);
            long solapados = ocupados.stream().filter(o -> o.overlaps(slotStart, slotEnd)).count();
            slots.add(new SlotDto(inicio.format(HORARIO), solapados < settings.capacidadeAtendimento()));
        }

        return new AvailabilityResponse(data, combo.totalDurationMinutes(), combo.totalPrice(), null, slots);
    }

    @Override
    @Transactional(readOnly = true)
    public ComboPricing resolveCombo(UUID tenantId, Long baseServiceId, List<Long> addonIds, Porte porte) {
        if (baseServiceId == null) {
            throw new IllegalArgumentException("Serviço base é obrigatório.");
        }
        if (porte == null) {
            throw new IllegalArgumentException("Porte é obrigatório.");
        }

        // Dedup preservando el orden: un adicional repetido se cobra una vez
        // (la PK compuesta de appointment_addons lo impide igual en la DB).
        SequencedSet<Long> addons = new LinkedHashSet<>(addonIds == null ? List.of() : addonIds);
        addons.remove(baseServiceId);

        SequencedSet<Long> pedidos = new LinkedHashSet<>();
        pedidos.add(baseServiceId);
        pedidos.addAll(addons);

        Map<Long, ServicePriceLine> lines = servicePricingRepository.findPriceLines(tenantId, pedidos, porte).stream()
                .collect(Collectors.toMap(ServicePriceLine::serviceId, Function.identity()));

        ServicePriceLine baseLine = lines.get(baseServiceId);
        if (baseLine == null) {
            throw new IllegalArgumentException(
                    "Serviço base não encontrado ou sem tarifa para o porte " + porte + ".");
        }
        if (baseLine.type() != ServiceType.BASE) {
            throw new IllegalArgumentException("O serviço " + baseServiceId + " não é um banho base.");
        }

        List<ComboPricing.Line> addonLines = new ArrayList<>(addons.size());
        BigDecimal total = baseLine.price();
        int duracao = baseLine.durationMinutes();

        for (Long addonId : addons) {
            ServicePriceLine line = lines.get(addonId);
            if (line == null) {
                throw new IllegalArgumentException(
                        "Adicional não encontrado ou sem tarifa para o porte " + porte + ": " + addonId + ".");
            }
            if (line.type() != ServiceType.ADDON) {
                throw new IllegalArgumentException("O serviço " + addonId + " não é um adicional.");
            }
            addonLines.add(new ComboPricing.Line(addonId, line.price(), line.durationMinutes()));
            total = total.add(line.price());
            duracao += line.durationMinutes();
        }

        return new ComboPricing(
                baseServiceId,
                baseLine.price(),
                baseLine.durationMinutes(),
                List.copyOf(addonLines),
                total.setScale(2, RoundingMode.HALF_UP),
                duracao);
    }

    private AvailabilityResponse unavailable(LocalDate data, ComboPricing combo, Indisponibilidade motivo) {
        return new AvailabilityResponse(
                data, combo.totalDurationMinutes(), combo.totalPrice(), motivo, List.of());
    }
}
