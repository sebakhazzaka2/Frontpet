package com.frontpet.booking;

import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.ScheduleBlock;
import com.frontpet.booking.domain.ScheduleBlockRepository;
import com.frontpet.booking.dto.BusinessHoursDetail;
import com.frontpet.booking.dto.CreateScheduleBlockRequest;
import com.frontpet.booking.dto.ScheduleBlockDetail;
import com.frontpet.booking.dto.UpsertBusinessHoursRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Escritura de {@code business_hours}/{@code schedule_blocks} (tarea 5.7).
 * Las validaciones de rango espejan los CHECK de {@code V3__booking.sql}
 * (ADR 020) — se repiten acá para devolver 400 con mensaje en PT-BR en vez de
 * dejar que una violación de constraint llegue como 500 genérico.
 */
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final BusinessHoursRepository businessHoursRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;

    @Override
    @Transactional
    public List<BusinessHoursDetail> upsertBusinessHours(UUID tenantId, List<UpsertBusinessHoursRequest> requests) {
        validateNoDuplicateDias(requests);

        List<BusinessHoursDetail> result = new ArrayList<>();
        for (UpsertBusinessHoursRequest request : requests) {
            validateRango(request);

            BusinessHours hours = businessHoursRepository.findByTenantIdAndDiaSemana(tenantId, request.diaSemana())
                    .orElseGet(() -> {
                        BusinessHours created = new BusinessHours();
                        created.setTenantId(tenantId);
                        created.setDiaSemana(request.diaSemana());
                        return created;
                    });

            hours.setActivo(request.activo());
            hours.setAbertura(request.abertura());
            hours.setFechamento(request.fechamento());
            hours.setPausaInicio(request.pausaInicio());
            hours.setPausaFin(request.pausaFin());
            businessHoursRepository.save(hours);

            result.add(toDetail(hours));
        }
        return result;
    }

    @Override
    @Transactional
    public ScheduleBlockDetail createScheduleBlock(UUID tenantId, CreateScheduleBlockRequest request) {
        if (request.dataDesde().isAfter(request.dataHasta())) {
            throw new IllegalArgumentException("Data de início deve ser anterior ou igual à data de fim.");
        }

        ScheduleBlock block = new ScheduleBlock();
        block.setTenantId(tenantId);
        block.setDataDesde(request.dataDesde());
        block.setDataHasta(request.dataHasta());
        block.setMotivo(request.motivo());
        scheduleBlockRepository.save(block);

        return toDetail(block);
    }

    @Override
    @Transactional
    public void deleteScheduleBlock(UUID tenantId, Long id) {
        ScheduleBlock block = scheduleBlockRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ScheduleBlockNotFoundException("Bloqueio não encontrado."));
        scheduleBlockRepository.delete(block);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessHoursDetail> listBusinessHours(UUID tenantId) {
        return businessHoursRepository.findByTenantIdOrderByDiaSemana(tenantId).stream()
                .map(this::toDetail)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleBlockDetail> listScheduleBlocks(UUID tenantId) {
        return scheduleBlockRepository.findByTenantIdOrderByDataDesde(tenantId).stream()
                .map(this::toDetail)
                .toList();
    }

    private void validateNoDuplicateDias(List<UpsertBusinessHoursRequest> requests) {
        Set<Short> seen = new HashSet<>();
        for (UpsertBusinessHoursRequest request : requests) {
            if (!seen.add(request.diaSemana())) {
                throw new IllegalArgumentException("Dia da semana duplicado no request: " + request.diaSemana() + ".");
            }
        }
    }

    private void validateRango(UpsertBusinessHoursRequest request) {
        if (!request.abertura().isBefore(request.fechamento())) {
            throw new IllegalArgumentException(
                    "Horário de abertura deve ser anterior ao de fechamento (dia " + request.diaSemana() + ").");
        }

        boolean pausaInicioNull = request.pausaInicio() == null;
        boolean pausaFinNull = request.pausaFin() == null;
        if (pausaInicioNull != pausaFinNull) {
            throw new IllegalArgumentException(
                    "Pausa incompleta: informe início e fim, ou nenhum dos dois (dia " + request.diaSemana() + ").");
        }
        if (!pausaInicioNull) {
            if (!request.pausaInicio().isBefore(request.pausaFin())) {
                throw new IllegalArgumentException(
                        "Início da pausa deve ser anterior ao fim (dia " + request.diaSemana() + ").");
            }
            if (request.pausaInicio().isBefore(request.abertura()) || request.pausaFin().isAfter(request.fechamento())) {
                throw new IllegalArgumentException(
                        "Pausa deve estar dentro do horário de atendimento (dia " + request.diaSemana() + ").");
            }
        }
    }

    private BusinessHoursDetail toDetail(BusinessHours hours) {
        return new BusinessHoursDetail(
                hours.getDiaSemana(), hours.getActivo(), hours.getAbertura(), hours.getFechamento(),
                hours.getPausaInicio(), hours.getPausaFin());
    }

    private ScheduleBlockDetail toDetail(ScheduleBlock block) {
        return new ScheduleBlockDetail(block.getId(), block.getDataDesde(), block.getDataHasta(), block.getMotivo());
    }
}
