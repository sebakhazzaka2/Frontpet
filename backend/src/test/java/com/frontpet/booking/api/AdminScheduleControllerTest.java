package com.frontpet.booking.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.domain.BusinessHours;
import com.frontpet.booking.domain.BusinessHoursRepository;
import com.frontpet.booking.domain.ScheduleBlockRepository;
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

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin de horarios y bloqueos (tarea 5.7, issue #51, Bloque G). Usa un
 * tenant propio (no el sembrado por V5) para no chocar con
 * {@code uq_business_hours_tenant_dia} — mismo criterio que
 * {@code BookingDomainIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminScheduleControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired BusinessHoursRepository businessHoursRepository;
    @Autowired ScheduleBlockRepository scheduleBlockRepository;

    private UUID tenantId;
    private AdminUser admin;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant Admin Schedule");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(Map.of());
        tenantRepository.save(tenant);
        tenantId = tenant.getId();
        admin = adminFor(tenantId);
    }

    @Test
    @DisplayName("PUT /business-hours sin cookie JWT devuelve 401")
    void upsertBusinessHoursRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/admin/business-hours")
                        .contentType("application/json")
                        .content(validBusinessHoursBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /business-hours sin cookie JWT devuelve 401")
    void listBusinessHoursRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/business-hours"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /business-hours devuelve lo que persistió el PUT, ordenado por diaSemana")
    void listsBusinessHoursAfterUpsert() throws Exception {
        mockMvc.perform(put("/api/v1/admin/business-hours")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(validBusinessHoursBody()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/business-hours")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].diaSemana").value(1))
                .andExpect(jsonPath("$[1].diaSemana").value(7));
    }

    @Test
    @DisplayName("GET /schedule-blocks lista los bloqueios creados, ordenados por dataDesde")
    void listsScheduleBlocks() throws Exception {
        String createBody = """
                { "dataDesde": "2026-09-07", "dataHasta": "2026-09-07", "motivo": "Feriado — Independência" }
                """;
        mockMvc.perform(post("/api/v1/admin/schedule-blocks")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/schedule-blocks")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].motivo").value("Feriado — Independência"));
    }

    @Test
    @DisplayName("PUT /business-hours hace upsert y se refleja en la DB")
    void upsertsBusinessHours() throws Exception {
        mockMvc.perform(put("/api/v1/admin/business-hours")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(validBusinessHoursBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].abertura").value("09:00:00"))
                .andExpect(jsonPath("$[1].activo").value(false));

        BusinessHours segunda = businessHoursRepository.findByTenantIdAndDiaSemana(tenantId, (short) 1).orElseThrow();
        assertThat(segunda.getAbertura()).isEqualTo(LocalTime.of(9, 0));
        assertThat(segunda.getFechamento()).isEqualTo(LocalTime.of(17, 0));

        BusinessHours domingo = businessHoursRepository.findByTenantIdAndDiaSemana(tenantId, (short) 7).orElseThrow();
        assertThat(domingo.getActivo()).isFalse();
    }

    @Test
    @DisplayName("PUT /business-hours con abertura >= fechamento devuelve 400 en PT-BR")
    void upsertWithInvalidRangeReturnsBadRequest() throws Exception {
        String body = """
                [ { "diaSemana": 1, "activo": true, "abertura": "17:00", "fechamento": "09:00", "pausaInicio": null, "pausaFin": null } ]
                """;

        mockMvc.perform(put("/api/v1/admin/business-hours")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("abertura")));
    }

    @Test
    @DisplayName("PUT /business-hours con pausa incoerente devuelve 400")
    void upsertWithIncoherentPauseReturnsBadRequest() throws Exception {
        String body = """
                [ { "diaSemana": 1, "activo": true, "abertura": "09:00", "fechamento": "17:00", "pausaInicio": "12:00", "pausaFin": null } ]
                """;

        mockMvc.perform(put("/api/v1/admin/business-hours")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /schedule-blocks crea y DELETE remove, reflejado en la DB")
    void createsAndDeletesScheduleBlock() throws Exception {
        String createBody = """
                { "dataDesde": "2026-09-07", "dataHasta": "2026-09-07", "motivo": "Feriado — Independência" }
                """;

        String response = mockMvc.perform(post("/api/v1/admin/schedule-blocks")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivo").value("Feriado — Independência"))
                .andReturn().getResponse().getContentAsString();

        assertThat(scheduleBlockRepository.findByTenantIdOrderByDataDesde(tenantId)).hasSize(1);

        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(delete("/api/v1/admin/schedule-blocks/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isNoContent());

        assertThat(scheduleBlockRepository.findByTenantIdOrderByDataDesde(tenantId)).isEmpty();
    }

    @Test
    @DisplayName("POST /schedule-blocks con dataDesde posterior a dataHasta devuelve 400")
    void createScheduleBlockWithInvalidRangeReturnsBadRequest() throws Exception {
        String body = """
                { "dataDesde": "2026-09-08", "dataHasta": "2026-09-07", "motivo": "Rango inválido" }
                """;

        mockMvc.perform(post("/api/v1/admin/schedule-blocks")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /schedule-blocks/{id} de outro tenant da 404")
    void deleteOtherTenantScheduleBlockIsNotFound() throws Exception {
        String createBody = """
                { "dataDesde": "2026-10-01", "dataHasta": "2026-10-01", "motivo": "Teste" }
                """;
        String response = mockMvc.perform(post("/api/v1/admin/schedule-blocks")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        AdminUser adminOtherTenant = adminFor(UUID.randomUUID());
        mockMvc.perform(delete("/api/v1/admin/schedule-blocks/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.user(adminOtherTenant)))
                .andExpect(status().isNotFound());
    }

    private String validBusinessHoursBody() {
        return """
                [
                  { "diaSemana": 1, "activo": true, "abertura": "09:00", "fechamento": "17:00", "pausaInicio": null, "pausaFin": null },
                  { "diaSemana": 7, "activo": false, "abertura": "09:00", "fechamento": "17:00", "pausaInicio": null, "pausaFin": null }
                ]
                """;
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
