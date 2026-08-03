package com.frontpet.booking;

import com.frontpet.booking.dto.BusinessHoursDetail;
import com.frontpet.booking.dto.CreateScheduleBlockRequest;
import com.frontpet.booking.dto.ScheduleBlockDetail;
import com.frontpet.booking.dto.UpsertBusinessHoursRequest;

import java.util.List;
import java.util.UUID;

/**
 * Puerta de entrada admin a horarios y bloqueos (ADR 012/020). No calcula
 * disponibilidad — eso es {@code AvailabilityService} (Bloque C); acá solo se
 * escriben los datos que ese cálculo lee.
 */
public interface ScheduleService {

    /**
     * Upsert de {@code business_hours} por {@code (tenant, diaSemana)}.
     *
     * @throws IllegalArgumentException si hay un {@code diaSemana} repetido
     *                                   en el request, o si alguna fila viola
     *                                   {@code abertura < fechamento} o la
     *                                   coherencia de pausa (mismas reglas
     *                                   que los CHECK de {@code V3__booking.sql},
     *                                   validadas antes en PT-BR)
     */
    List<BusinessHoursDetail> upsertBusinessHours(UUID tenantId, List<UpsertBusinessHoursRequest> requests);

    /**
     * Bloquea un rango de días completos (feriados, u ocupación por otros
     * canales — ADR 012). No es por horario, limitación conocida (ADR 020).
     *
     * @throws IllegalArgumentException si {@code dataDesde > dataHasta}
     */
    ScheduleBlockDetail createScheduleBlock(UUID tenantId, CreateScheduleBlockRequest request);

    /** @throws ScheduleBlockNotFoundException se o bloqueio não existe ou não pertence ao tenant */
    void deleteScheduleBlock(UUID tenantId, Long id);

    /** Los 7 días existentes, ordenados por {@code diaSemana} ISO (1=Lun..7=Dom). */
    List<BusinessHoursDetail> listBusinessHours(UUID tenantId);

    /** Bloqueios ordenados por {@code dataDesde}, para poder listarlos y borrarlos. */
    List<ScheduleBlockDetail> listScheduleBlocks(UUID tenantId);
}
