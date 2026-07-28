package com.frontpet.config;

import com.frontpet.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /actuator/health} es {@code permitAll} (tarea 1.7) — su cuerpo es
 * público para cualquiera en internet, incluido un bot. El assert que más
 * importa acá no es que responda 200: es que {@code $.components} no exista,
 * porque sin {@code management.endpoint.health.show-details: never} este
 * endpoint publica el estado de la conexión a la DB a quien lo pida.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health sin cookie devuelve 200 UP, sin detalle de componentes")
    void healthIsPublicAndHidesDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("GET /actuator/health/readiness sin cookie devuelve 200 (probe para Coolify)")
    void readinessProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/metrics sin cookie devuelve 401 — no está expuesto ni es público")
    void metricsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }
}
