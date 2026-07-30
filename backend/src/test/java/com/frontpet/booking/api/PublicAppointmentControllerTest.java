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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/v1/appointments} y la vista pública reducida (tarea 5.5)
 * sobre el stack HTTP real. Desactiva el rate limit (varios POSTs por clase)
 * — se cubre aparte en {@code AppointmentRateLimitIntegrationTest}.
 *
 * <p>Tenant fijo de {@code frontpet.tenant.id} (V5): capacidade 2,
 * Seg-Sex 09:00-17:00, mismo criterio que {@code PublicAvailabilityControllerTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "frontpet.appointment-rate-limit.enabled=false")
class PublicAppointmentControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Value("${frontpet.tenant.id}") UUID tenantId;

    private ServiceOffering banhoBase;

    @BeforeEach
    void seed() {
        banhoBase = new ServiceOffering();
        banhoBase.setTenantId(tenantId);
        banhoBase.setType(ServiceType.BASE);
        banhoBase.setNome("Banho Reserva Teste");
        serviceOfferingRepository.save(banhoBase);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(banhoBase);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        servicePricingRepository.save(pricing);
    }

    @Test
    @DisplayName("POST /appointments sin cookie devuelve 201 e persiste os snapshots server-side")
    void createsAppointmentWithoutAuthentication() throws Exception {
        String body = """
                {
                  "baseServiceId": %d,
                  "addonIds": [],
                  "porte": "M",
                  "data": "%s",
                  "horario": "10:00",
                  "clienteNome": "Ana Souza",
                  "clienteTelefone": "(55) 99123-4567",
                  "petNome": "Thor",
                  "petRaca": "Vira-lata"
                }
                """.formatted(banhoBase.getId(), proximaQuarta());

        String response = mockMvc.perform(post("/api/v1/appointments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.baseServiceNome").value("Banho Reserva Teste"))
                .andExpect(jsonPath("$.totalPriceSnapshot").value(59.00))
                .andExpect(jsonPath("$.totalDurationMinutes").value(60))
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        UUID publicId = UUID.fromString(response.replaceAll(".*\"publicId\":\"([^\"]+)\".*", "$1"));
        Appointment saved = appointmentRepository.findByTenantIdAndPublicId(tenantId, publicId).orElseThrow();
        assertThat(saved.getClienteTelefoneNorm()).isEqualTo("5555991234567");
    }

    @Test
    @DisplayName("honeypot preenchido devuelve 400 (mismo patrão anti-bot que orders)")
    void honeypotFilledReturnsBadRequest() throws Exception {
        String body = """
                {
                  "baseServiceId": %d,
                  "porte": "M",
                  "data": "%s",
                  "horario": "10:00",
                  "clienteNome": "Bot",
                  "clienteTelefone": "(55) 99123-4567",
                  "petNome": "Thor",
                  "honeypot": "preenchido"
                }
                """.formatted(banhoBase.getId(), proximaQuarta());

        mockMvc.perform(post("/api/v1/appointments").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("horário fora da grilla de 30 min devuelve 400")
    void invalidHorarioReturnsBadRequest() throws Exception {
        String body = """
                {
                  "baseServiceId": %d,
                  "porte": "M",
                  "data": "%s",
                  "horario": "10:15",
                  "clienteNome": "Ana Souza",
                  "clienteTelefone": "(55) 99123-4567",
                  "petNome": "Thor"
                }
                """.formatted(banhoBase.getId(), proximaQuarta());

        mockMvc.perform(post("/api/v1/appointments").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sem cupo disponível (capacidade 2 já ocupada) devuelve 409")
    void slotWithoutCapacityReturns409() throws Exception {
        LocalDate quarta = proximaQuarta();
        turno(quarta, LocalTime.of(10, 0), AppointmentStatus.CONFIRMED);
        turno(quarta, LocalTime.of(10, 0), AppointmentStatus.PENDING);

        String body = """
                {
                  "baseServiceId": %d,
                  "porte": "M",
                  "data": "%s",
                  "horario": "10:00",
                  "clienteNome": "Terceiro Cliente",
                  "clienteTelefone": "(55) 99123-4567",
                  "petNome": "Rex"
                }
                """.formatted(banhoBase.getId(), quarta);

        mockMvc.perform(post("/api/v1/appointments").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /appointments/{publicId} devuelve la vista reducida, sem telefone")
    void getPublicByPublicIdReturnsReducedView() throws Exception {
        Appointment appointment = turno(proximaQuarta(), LocalTime.of(11, 0), AppointmentStatus.PENDING);

        mockMvc.perform(get("/api/v1/appointments/" + appointment.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(appointment.getPublicId().toString()))
                .andExpect(jsonPath("$.petNome").value("Thor"))
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist());
    }

    @Test
    @DisplayName("GET /appointments/{publicId} inexistente devuelve 404")
    void getUnknownPublicIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
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
}
