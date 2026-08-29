package com.frontpet.booking;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.booking.dto.AppointmentCounts;
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
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Happy path de {@code AppointmentService.countsForAdmin} (mini-dashboard,
 * ROADMAP tarea 7.1/7.3) contra Postgres real. Usa un tenant propio, no el
 * sembrado por V5, para no depender ni chocar con turnos de otros testes.
 */
@SpringBootTest
@Transactional
class AppointmentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired AppointmentService appointmentService;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired TenantRepository tenantRepository;

    private UUID tenantId;
    private Long baseServiceId;

    @BeforeEach
    void createTenantAndService() {
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant Dashboard Teste");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(Map.of("capacidade_atendimento", 2));
        tenantRepository.save(tenant);
        tenantId = tenant.getId();

        ServiceOffering base = new ServiceOffering();
        base.setTenantId(tenantId);
        base.setType(ServiceType.BASE);
        base.setNome("Banho Teste Dashboard");
        serviceOfferingRepository.save(base);
        baseServiceId = base.getId();
    }

    @Test
    @DisplayName("conta turnos de hoje, dos próximos 7 dias e os que aguardam confirmação, excluindo CANCELLED")
    void countsForAdminHappyPath() {
        LocalDate hoje = LocalDate.now(SlotGrid.ZONE_ID);

        persistAppointment(hoje, AppointmentStatus.PENDING);   // hoje + aguardando
        persistAppointment(hoje, AppointmentStatus.CONFIRMED); // hoje
        persistAppointment(hoje, AppointmentStatus.CANCELLED); // excluído
        persistAppointment(hoje.plusDays(3), AppointmentStatus.PENDING); // próximos 7 dias + aguardando
        persistAppointment(hoje.plusDays(10), AppointmentStatus.CONFIRMED); // fora da janela

        AppointmentCounts counts = appointmentService.countsForAdmin(tenantId);

        assertThat(counts.hoje()).isEqualTo(2);
        assertThat(counts.proximos7Dias()).isEqualTo(1);
        assertThat(counts.aguardandoConfirmacao()).isEqualTo(2);
    }

    private void persistAppointment(LocalDate data, AppointmentStatus status) {
        Instant startAt = ZonedDateTime.of(data, LocalTime.of(10, 0), SlotGrid.ZONE_ID).toInstant();

        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(tenantId);
        ServiceOffering base = serviceOfferingRepository.findById(baseServiceId).orElseThrow();
        appointment.setBaseService(base);
        appointment.setSize(Porte.M);
        appointment.setStartAt(startAt);
        appointment.setEndAt(startAt.plusSeconds(3600));
        appointment.setClienteNome("Cliente Teste");
        appointment.setClienteTelefone("+55 55 99123-4567");
        appointment.setClienteTelefoneNorm("5555991234567");
        appointment.setPetNome("Rex");
        appointment.setBasePriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalPriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalDurationMinutes(60);
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }
}
