package com.frontpet.identity;

import com.frontpet.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link LoginRateLimitFilter} sobre el stack HTTP real (tarea 1.8, ADR 019).
 * Complementa a {@code AuthControllerIntegrationTest} (paquete {@code .api}),
 * que no toca rate limit. Vive en {@code com.frontpet.identity} y no en
 * {@code .api} porque necesita el {@code clear()} package-private de
 * {@link LoginAttemptService} — exponerlo público solo para que el test
 * quedara en el paquete "correcto" hubiera ensanchado la API real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Value("${frontpet.admin.email}")
    private String adminEmail;

    @Value("${frontpet.admin.password}")
    private String adminPassword;

    @Autowired private MockMvc mockMvc;
    @Autowired private LoginAttemptService loginAttempts;
    @Autowired private LoginRateLimitProperties props;

    @BeforeEach
    void clearCounters() {
        // El bean es singleton y Spring cachea el ApplicationContext entre
        // clases de test: sin esto, los fallos de esta clase bloquearían a
        // AuthControllerIntegrationTest (que loguea con el mismo admin
        // sembrado por DataInitializer) según el orden en que corran.
        loginAttempts.clear();
    }

    @Test
    @DisplayName("al superar maxFailures desde una IP, el siguiente intento da 429 con Retry-After")
    void exceedingMaxFailuresBlocksTheIp() throws Exception {
        String ip = "203.0.113.20";
        for (int i = 0; i < props.maxFailures(); i++) {
            mockMvc.perform(loginFrom(ip, adminEmail, "senha-errada"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginFrom(ip, adminEmail, "senha-errada"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value(LOGIN_URL));
    }

    @Test
    @DisplayName("un login exitoso resetea el contador de fallos de esa IP")
    void successfulLoginResetsCounter() throws Exception {
        String ip = "203.0.113.21";
        for (int i = 0; i < props.maxFailures() - 1; i++) {
            mockMvc.perform(loginFrom(ip, adminEmail, "senha-errada"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginFrom(ip, adminEmail, adminPassword))
                .andExpect(status().isOk());

        // Si el reset no hubiera corrido, este sería el fallo número
        // maxFailures + 1 acumulado y ya estaría bloqueado.
        mockMvc.perform(loginFrom(ip, adminEmail, "senha-errada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un 400 de validación no consume presupuesto del rate limit")
    void validationFailureDoesNotConsumeBudget() throws Exception {
        String ip = "203.0.113.22";
        for (int i = 0; i < props.maxFailures() + 5; i++) {
            mockMvc.perform(loginFrom(ip, "", adminPassword))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(loginFrom(ip, adminEmail, adminPassword))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder loginFrom(String ip, String email, String password) {
        return post(LOGIN_URL)
                .with(fromIp(ip))
                .contentType("application/json")
                .content(loginBody(email, password));
    }

    private static RequestPostProcessor fromIp(String ip) {
        // MockMvc pone 127.0.0.1 por defecto en TODOS los tests: sin variar
        // la IP, los tests de esta clase se bloquearían entre sí.
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
