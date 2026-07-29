package com.frontpet.booking;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentAddonLine;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ScheduleBlock;
import com.frontpet.booking.domain.ScheduleBlockRepository;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.ServicePricingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.common.UuidV7;
import com.frontpet.tenant.domain.Tenant;
import com.frontpet.tenant.domain.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip de las entidades del Bloque A (5.2) contra {@code V3__booking.sql}
 * bajo {@code ddl-auto: validate}. No hay lógica de negocio todavía — eso es
 * de {@code AvailabilityServiceImpl}/{@code AppointmentServiceImpl} (Bloque C/E).
 *
 * <p>Usa un tenant propio, no el sembrado por V5: ese ya trae {@code business_hours}
 * para los 7 días, y reusarlo chocaría con {@code uq_business_hours_tenant_dia}.
 */
@SpringBootTest
@Transactional
class BookingDomainIntegrationTest extends AbstractIntegrationTest {

    @Autowired TenantRepository tenantRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Autowired BusinessHoursRepository businessHoursRepository;
    @Autowired ScheduleBlockRepository scheduleBlockRepository;
    @Autowired AppointmentRepository appointmentRepository;

    private UUID TENANT;

    @BeforeEach
    void createTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant de Teste");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(Map.of("capacidade_atendimento", 2));
        tenantRepository.save(tenant);
        TENANT = tenant.getId();
    }

    @Test
    @DisplayName("services + service_pricing: persiste y recupera tarifa por porte")
    void servicePricingRoundTrip() {
        ServiceOffering base = new ServiceOffering();
        base.setTenantId(TENANT);
        base.setType(ServiceType.BASE);
        base.setNome("Banho Teste");
        base.setDescricao("Descrição de teste.");
        serviceOfferingRepository.save(base);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(base);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        servicePricingRepository.save(pricing);

        ServicePricing found = servicePricingRepository.findByServiceIdAndSize(base.getId(), Porte.M)
                .orElseThrow();
        assertThat(found.getPrice()).isEqualByComparingTo("59.00");
        assertThat(found.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("business_hours: upsert por (tenant, dia_semana) e pausa NULL (caso FrontPet)")
    void businessHoursRoundTrip() {
        BusinessHours hours = new BusinessHours();
        hours.setTenantId(TENANT);
        hours.setDiaSemana((short) 1);
        hours.setActivo(true);
        hours.setAbertura(LocalTime.of(9, 0));
        hours.setFechamento(LocalTime.of(17, 0));
        businessHoursRepository.save(hours);

        BusinessHours found = businessHoursRepository.findByTenantIdAndDiaSemana(TENANT, (short) 1)
                .orElseThrow();
        assertThat(found.getPausaInicio()).isNull();
        assertThat(found.getAbertura()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("schedule_blocks: encuentra el bloqueo que cubre una fecha")
    void scheduleBlockCoversDate() {
        ScheduleBlock block = new ScheduleBlock();
        block.setTenantId(TENANT);
        block.setDataDesde(LocalDate.of(2026, 9, 7));
        block.setDataHasta(LocalDate.of(2026, 9, 7));
        block.setMotivo("Feriado de teste");
        scheduleBlockRepository.save(block);

        LocalDate consultada = LocalDate.of(2026, 9, 7);
        assertThat(scheduleBlockRepository
                .findFirstByTenantIdAndDataDesdeLessThanEqualAndDataHastaGreaterThanEqual(TENANT, consultada, consultada))
                .isPresent();

        LocalDate outraData = LocalDate.of(2026, 9, 8);
        assertThat(scheduleBlockRepository
                .findFirstByTenantIdAndDataDesdeLessThanEqualAndDataHastaGreaterThanEqual(TENANT, outraData, outraData))
                .isEmpty();
    }

    @Test
    @DisplayName("appointments + appointment_addons: persiste el turno con sus adicionais (@ElementCollection)")
    void appointmentWithAddonsRoundTrip() {
        ServiceOffering base = new ServiceOffering();
        base.setTenantId(TENANT);
        base.setType(ServiceType.BASE);
        base.setNome("Banho Base Teste");
        serviceOfferingRepository.save(base);

        ServiceOffering addon = new ServiceOffering();
        addon.setTenantId(TENANT);
        addon.setType(ServiceType.ADDON);
        addon.setNome("Adicional Teste");
        serviceOfferingRepository.save(addon);

        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(TENANT);
        appointment.setBaseService(base);
        appointment.setSize(Porte.M);
        appointment.setStartAt(Instant.parse("2026-08-05T12:30:00Z"));
        appointment.setEndAt(Instant.parse("2026-08-05T13:39:00Z"));
        appointment.setClienteNome("Ana Souza");
        appointment.setClienteTelefone("+55 55 99123-4567");
        appointment.setClienteTelefoneNorm("5555991234567");
        appointment.setPetNome("Thor");
        appointment.setBasePriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalPriceSnapshot(new BigDecimal("79.00"));
        appointment.setTotalDurationMinutes(80);
        appointment.getAddons().add(new AppointmentAddonLine(addon.getId(), new BigDecimal("20.00"), 20));
        appointmentRepository.save(appointment);

        Appointment found = appointmentRepository.findByTenantIdAndPublicId(TENANT, appointment.getPublicId())
                .orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(found.getTempoExtra()).isFalse();
        assertThat(found.getAddons()).hasSize(1);
        assertThat(found.getAddons().get(0).getServiceId()).isEqualTo(addon.getId());
        assertThat(found.getAddons().get(0).getPriceSnapshot()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("service_pricing: findByServiceIdInAndSize resuelve varios serviços de una vez (combo base+adicionais)")
    void findByServiceIdInAndSizeResolvesCombo() {
        ServiceOffering base = new ServiceOffering();
        base.setTenantId(TENANT);
        base.setType(ServiceType.BASE);
        base.setNome("Base Combo Teste");
        serviceOfferingRepository.save(base);

        ServiceOffering addon = new ServiceOffering();
        addon.setTenantId(TENANT);
        addon.setType(ServiceType.ADDON);
        addon.setNome("Addon Combo Teste");
        serviceOfferingRepository.save(addon);

        ServicePricing baseGPricing = new ServicePricing();
        baseGPricing.setService(base);
        baseGPricing.setSize(Porte.G);
        baseGPricing.setPrice(new BigDecimal("79.00"));
        baseGPricing.setDurationMinutes(90);
        servicePricingRepository.save(baseGPricing);

        ServicePricing addonGPricing = new ServicePricing();
        addonGPricing.setService(addon);
        addonGPricing.setSize(Porte.G);
        addonGPricing.setPrice(new BigDecimal("25.00"));
        addonGPricing.setDurationMinutes(25);
        servicePricingRepository.save(addonGPricing);

        List<ServicePricing> combo = servicePricingRepository.findByServiceIdInAndSize(
                List.of(base.getId(), addon.getId()), Porte.G);

        assertThat(combo).hasSize(2);
        assertThat(combo.stream().mapToInt(ServicePricing::getDurationMinutes).sum()).isEqualTo(115);
    }
}
