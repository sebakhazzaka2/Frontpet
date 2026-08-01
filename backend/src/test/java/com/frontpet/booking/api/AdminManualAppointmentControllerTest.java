package com.frontpet.booking.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.SlotGrid;
import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ScheduleBlock;
import com.frontpet.booking.domain.ScheduleBlockRepository;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.ServicePricingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.common.UuidV7;
import com.frontpet.identity.domain.AdminUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /admin/appointments} (turno manual, ADR 021) e {@code GET
 * /admin/appointments} por rango de {@code desde}/{@code hasta} (Bloque A,
 * Sprint 6). Usa o tenant fixo de {@code frontpet.tenant.id} (V5 seed:
 * Seg-Sex 09:00-17:00, capacidade 2) — mesmo critério que
 * {@code PublicAppointmentControllerTest} — para que "fora da grilha" e
 * "supera capacidade" não disparem também o aviso de "fora do horário"
 * por falta de business_hours.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminManualAppointmentControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired ScheduleBlockRepository scheduleBlockRepository;
    @Autowired EntityManager entityManager;
    @Value("${frontpet.tenant.id}") UUID tenantId;

    private AdminUser admin;
    private ServiceOffering banhoBase;

    @BeforeEach
    void setUp() {
        admin = adminFor(tenantId);

        banhoBase = new ServiceOffering();
        banhoBase.setTenantId(tenantId);
        banhoBase.setType(ServiceType.BASE);
        banhoBase.setNome("Banho Manual Teste");
        serviceOfferingRepository.save(banhoBase);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(banhoBase);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        servicePricingRepository.save(pricing);
    }

    @Test
    @DisplayName("POST /admin/appointments fora da grilha de 30 min persiste (sem 400)")
    void manualOutsideGridStillPersists() throws Exception {
        mockMvc.perform(post("/api/v1/admin/appointments")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(bodyFor(proximaQuarta(), "10:17")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPriceSnapshot").value(59.00));
    }

    @Test
    @DisplayName("POST /admin/appointments que supera capacidade persiste com aviso, não 409")
    void manualOverCapacityPersistsWithWarning() throws Exception {
        LocalDate quarta = proximaQuarta();
        turno(quarta, LocalTime.of(10, 0), AppointmentStatus.CONFIRMED);
        turno(quarta, LocalTime.of(10, 0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(post("/api/v1/admin/appointments")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(bodyFor(quarta, "10:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("capacidade"))));
    }

    @Test
    @DisplayName("POST /admin/appointments em dia bloqueado persiste com aviso, não 400")
    void manualOnBlockedDayPersistsWithWarning() throws Exception {
        LocalDate quarta = proximaQuarta();

        ScheduleBlock block = new ScheduleBlock();
        block.setTenantId(tenantId);
        block.setDataDesde(quarta);
        block.setDataHasta(quarta);
        block.setMotivo("Feriado de teste");
        scheduleBlockRepository.save(block);

        mockMvc.perform(post("/api/v1/admin/appointments")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(bodyFor(quarta, "10:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("bloqueada"))));
    }

    @Test
    @DisplayName("GET /admin/appointments?desde=&hasta= lista os turnos da janela de 7 dias")
    void listsByDateRange() throws Exception {
        LocalDate quarta = proximaQuarta();
        turno(quarta, LocalTime.of(10, 0), AppointmentStatus.PENDING);
        turno(quarta.plusDays(2), LocalTime.of(11, 0), AppointmentStatus.CONFIRMED);
        turno(quarta.plusDays(10), LocalTime.of(11, 0), AppointmentStatus.PENDING); // fora da janela

        mockMvc.perform(get("/api/v1/admin/appointments")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .param("desde", quarta.toString())
                        .param("hasta", quarta.plusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /admin/appointments?desde=&hasta=&status= filtra por status dentro da janela")
    void listsByDateRangeAndStatus() throws Exception {
        LocalDate quarta = proximaQuarta();
        turno(quarta, LocalTime.of(10, 0), AppointmentStatus.PENDING);
        turno(quarta.plusDays(2), LocalTime.of(11, 0), AppointmentStatus.CONFIRMED);

        mockMvc.perform(get("/api/v1/admin/appointments")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .param("desde", quarta.toString())
                        .param("hasta", quarta.plusDays(6).toString())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    private String bodyFor(LocalDate data, String horario) {
        return """
                {
                  "baseServiceId": %d,
                  "porte": "M",
                  "data": "%s",
                  "horario": "%s",
                  "clienteNome": "Cliente por Telefone",
                  "clienteTelefone": "(55) 99123-4567",
                  "petNome": "Rex"
                }
                """.formatted(banhoBase.getId(), data, horario);
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
        user.setEmail("admin-manual-test@frontpet.com.br");
        user.setPasswordHash("{bcrypt}$2a$10$fakehashfortestingonly");
        user.setRole("ADMIN");
        return user;
    }
}
