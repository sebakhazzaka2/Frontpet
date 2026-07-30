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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired EntityManager entityManager;

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

        // Porte G a 90 min: es el caso 90 → 108 que pide literalmente el AC 13.
        ServicePricing pricingG = new ServicePricing();
        pricingG.setService(banhoBase);
        pricingG.setSize(Porte.G);
        pricingG.setPrice(new BigDecimal("79.00"));
        pricingG.setDurationMinutes(90);
        servicePricingRepository.save(pricingG);
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
    @DisplayName("confirmar sella confirmed_at; repetir la confirmación no lo re-sella")
    void confirmSealsTimestampOnceOnly() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.PENDING);
        assertThat(appointment.getConfirmedAt()).isNull();

        patchStatus(appointment, AppointmentStatus.CONFIRMED);
        Instant primeiroSelo = recarregar(appointment).getConfirmedAt();
        assertThat(primeiroSelo).as("confirmed_at sellado al confirmar").isNotNull();

        // Segunda confirmación: no-op idempotente. Si el service volviera a
        // sellar, el timestamp cambiaría y perderíamos el registro de cuándo
        // se confirmó realmente.
        patchStatus(appointment, AppointmentStatus.CONFIRMED);
        assertThat(recarregar(appointment).getConfirmedAt())
                .as("confirmed_at no se re-sella en el no-op")
                .isEqualTo(primeiroSelo);
    }

    @Test
    @DisplayName("cancelar un turno CONFIRMED sella cancelled_at y deja confirmed_at intacto")
    void cancelSealsCancelledAtKeepingConfirmedAt() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(10, 0), AppointmentStatus.PENDING);

        patchStatus(appointment, AppointmentStatus.CONFIRMED);
        Instant confirmadoEm = recarregar(appointment).getConfirmedAt();

        patchStatus(appointment, AppointmentStatus.CANCELLED);
        Appointment cancelado = recarregar(appointment);

        assertThat(cancelado.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelado.getCancelledAt()).isNotNull();
        assertThat(cancelado.getConfirmedAt()).isEqualTo(confirmadoEm);
    }

    private void patchStatus(Appointment appointment, AppointmentStatus status) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"status\": \"" + status + "\"}"))
                .andExpect(status().isOk());
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

        // El AC 13 dice "avisa, pero persiste igual": sin este assert, una
        // implementación que arma bien el DTO y no escribe nada pasaría el
        // test de arriba sin que nadie se entere.
        assertThat(recarregar(appointment).getTotalDurationMinutes()).isEqualTo(72);
    }

    @Test
    @DisplayName("PATCH /tempo-extra persiste duração e fim na DB, não só na resposta")
    void tempoExtraIsPersisted() throws Exception {
        Appointment appointment = turnoPorteG(proximaQuarta(), LocalTime.of(10, 0));
        Instant inicio = appointment.getStartAt();

        // 90 → 108, el caso literal del AC 13.
        mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/tempo-extra")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"tempoExtra\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(108));

        Appointment persistido = recarregar(appointment);
        assertThat(persistido.getTempoExtra()).isTrue();
        assertThat(persistido.getTotalDurationMinutes()).isEqualTo(108);
        assertThat(persistido.getEndAt()).isEqualTo(inicio.plus(108, ChronoUnit.MINUTES));
        // El preço NUNCA cambia con tempo_extra (ADR 011).
        assertThat(persistido.getTotalPriceSnapshot()).isEqualByComparingTo("79.00");
    }

    @Test
    @DisplayName("aplicar tempo-extra dos veces no acumula: 90 → 108, no 108 → 130")
    void tempoExtraIsIdempotentAndReversible() throws Exception {
        Appointment appointment = turnoPorteG(proximaQuarta(), LocalTime.of(10, 0));

        patchTempoExtra(appointment, true).andExpect(jsonPath("$.totalDurationMinutes").value(108));
        // Segunda vez: si el recálculo partiera de total_duration_minutes ya
        // guardado en vez del combo vigente, daría 108*1.2 = 130. Es el motivo
        // por el que updateTempoExtra resuelve el combo de nuevo en cada llamada.
        patchTempoExtra(appointment, true).andExpect(jsonPath("$.totalDurationMinutes").value(108));

        // Y desactivar vuelve exactamente a la duración base, sin drift.
        patchTempoExtra(appointment, false).andExpect(jsonPath("$.totalDurationMinutes").value(90));

        Appointment persistido = recarregar(appointment);
        assertThat(persistido.getTempoExtra()).isFalse();
        assertThat(persistido.getTotalDurationMinutes()).isEqualTo(90);
    }

    private ResultActions patchTempoExtra(Appointment appointment, boolean tempoExtra) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/appointments/" + appointment.getPublicId() + "/tempo-extra")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"tempoExtra\": " + tempoExtra + "}"))
                .andExpect(status().isOk());
    }

    /**
     * Fuerza el UPDATE contra la DB y vacía la persistence context antes de
     * releer. Sin el {@code flush}/{@code clear}, el {@code findBy...}
     * devolvería la misma instancia managed que ya tiene los campos seteados en
     * memoria — y el assert pasaría aunque el UPDATE nunca llegara a Postgres.
     */
    private Appointment recarregar(Appointment appointment) {
        entityManager.flush();
        entityManager.clear();
        return appointmentRepository.findByTenantIdAndPublicId(tenantId, appointment.getPublicId()).orElseThrow();
    }

    private LocalDate proximaQuarta() {
        LocalDate data = LocalDate.now(SlotGrid.ZONE_ID).plusDays(7);
        while (data.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            data = data.plusDays(1);
        }
        return data;
    }

    private Appointment turno(LocalDate data, LocalTime hora, AppointmentStatus status) {
        return turno(data, hora, status, Porte.M, 60, "59.00");
    }

    /** Turno de porte G (90 min) — el caso 90 → 108 del AC 13. */
    private Appointment turnoPorteG(LocalDate data, LocalTime hora) {
        return turno(data, hora, AppointmentStatus.PENDING, Porte.G, 90, "79.00");
    }

    private Appointment turno(LocalDate data, LocalTime hora, AppointmentStatus status,
                              Porte porte, int duracaoMinutes, String preco) {
        Instant inicio = ZonedDateTime.of(data, hora, SlotGrid.ZONE_ID).toInstant();

        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(tenantId);
        appointment.setBaseService(banhoBase);
        appointment.setSize(porte);
        appointment.setStartAt(inicio);
        appointment.setEndAt(inicio.plus(duracaoMinutes, ChronoUnit.MINUTES));
        appointment.setStatus(status);
        appointment.setClienteNome("Cliente de Teste");
        appointment.setClienteTelefone("+55 55 99123-4567");
        appointment.setClienteTelefoneNorm("5555991234567");
        appointment.setPetNome("Thor");
        appointment.setBasePriceSnapshot(new BigDecimal(preco));
        appointment.setTotalPriceSnapshot(new BigDecimal(preco));
        appointment.setTotalDurationMinutes(duracaoMinutes);
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
