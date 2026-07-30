package com.frontpet.booking.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.SlotGrid;
import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.ServicePricingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.common.UuidV7;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.tenant.domain.Tenant;
import com.frontpet.tenant.domain.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin de turnos (tareas 5.5/5.6) sobre el stack HTTP real. Mismo patrón que
 * {@code AdminServiceControllerTest}: {@code AdminUser} en memoria, sin login
 * real; tenant propio (el tenantId sale de {@code AdminUser}, no de
 * {@code CurrentTenant} — puede ser cualquiera).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAppointmentControllerTest extends AbstractIntegrationTest {

    private static final int CAPACIDADE = 2;

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Autowired AppointmentRepository appointmentRepository;

    private UUID tenantId;
    private AdminUser admin;
    private ServiceOffering banhoBase;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant Admin Appointments");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(Map.of(
                "capacidade_atendimento", CAPACIDADE,
                "anticipacao_min_horas", 1,
                "anticipacao_max_dias", 60));
        tenantRepository.save(tenant);
        tenantId = tenant.getId();
        admin = adminFor(tenantId);

        banhoBase = new ServiceOffering();
        banhoBase.setTenantId(tenantId);
        banhoBase.setType(ServiceType.BASE);
        banhoBase.setNome("Banho Admin Teste");
        serviceOfferingRepository.save(banhoBase);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(banhoBase);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        servicePricingRepository.save(pricing);
    }

    @Test
    @DisplayName("GET /admin/appointments sin cookie devuelve 401")
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/appointments devuelve o turno com telefone completo e nome do serviço")
    void listsAppointmentsWithFullDetail() throws Exception {
        turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.PENDING);

        mockMvc.perform(get("/api/v1/admin/appointments").with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clienteTelefone").value("+55 55 99123-4567"))
                .andExpect(jsonPath("$[0].baseServiceNome").value("Banho Admin Teste"));
    }

    @Test
    @DisplayName("PATCH /status de PENDING a CONFIRMED sella confirmed_at")
    void confirmsAppointment() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.PENDING);

        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /status de CONFIRMED a PENDING é transição inválida, 400")
    void rejectsInvalidTransition() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /status repetindo o mesmo status é no-op idempotente")
    void sameStatusIsNoOp() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.PENDING);

        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /tempo-extra recalcula duração (+20%) e fim, sem aviso quando não há solapamento")
    void appliesTempoExtraWithoutWarning() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.PENDING);

        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/tempo-extra")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"tempoExtra\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(72))   // 60 * 1.2
                .andExpect(jsonPath("$.aviso").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /tempo-extra avisa (mas persiste) quando o novo fim supera a capacidade")
    void appliesTempoExtraWithWarningWhenOverCapacity() throws Exception {
        LocalDate quarta = proximaQuarta();
        Appointment appointment = turno(quarta, LocalTime.of(10, 0), AppointmentStatus.PENDING);
        // Ocupa exactamente los dos cupos entre 11:00 y 11:12 (10:00+60=11:00,
        // el +20% lo extiende a 11:12) para que el recálculo choque.
        turno(quarta, LocalTime.of(11, 0), AppointmentStatus.CONFIRMED);
        turno(quarta, LocalTime.of(11, 0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/tempo-extra")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"tempoExtra\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(72))
                .andExpect(jsonPath("$.aviso").isNotEmpty());
    }

    private LocalDate proximaQuarta() {
        LocalDate data = LocalDate.now(SlotGrid.ZONE_ID).plusDays(7);
        while (data.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            data = data.plusDays(1);
        }
        return data;
    }

    private Appointment turno(LocalDate data, LocalTime hora, AppointmentStatus status) {
        Instant inicio = ZonedDateTime.of(data, hora, SlotGrid.ZONE_ID).toInstant();

        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(tenantId);
        appointment.setBaseService(banhoBase);
        appointment.setSize(Porte.M);
        appointment.setStartAt(inicio);
        appointment.setEndAt(inicio.plus(60, ChronoUnit.MINUTES));
        appointment.setStatus(status);
        appointment.setClienteNome("Cliente de Teste");
        appointment.setClienteTelefone("+55 55 99123-4567");
        appointment.setClienteTelefoneNorm("5555991234567");
        appointment.setPetNome("Thor");
        appointment.setBasePriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalPriceSnapshot(new BigDecimal("59.00"));
        appointment.setTotalDurationMinutes(60);
        return appointmentRepository.save(appointment);
    }

    private static AdminUser adminFor(UUID tenantId) {
        AdminUser user = new AdminUser();
        user.setId(1L);
        user.setTenantId(tenantId);
        user.setEmail("admin-test@frontpet.com.br");
        user.setPasswordHash("{bcrypt}$2a$10$fakehashfortestingonly");
        user.setRole("ADMIN");
        return user;
    }
}
