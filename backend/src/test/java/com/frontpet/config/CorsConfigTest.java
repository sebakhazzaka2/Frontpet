package com.frontpet.config;

import com.frontpet.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS visto desde el navegador.
 *
 * <p>Estos tests son de los que más valen: el backend puede contestar 200
 * perfecto y aun así el frontend no ver nada, porque quien bloquea es el
 * navegador al no encontrar los headers. Un test de endpoint normal jamás
 * lo detecta — no manda {@code Origin}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest extends AbstractIntegrationTest {

    private static final String FRONTEND = "http://localhost:3000";

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("el preflight OPTIONS pasa sin auth y autoriza el origen")
    void allowsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", FRONTEND)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("un GET real desde el frontend trae el header de origen permitido")
    void allowsSimpleRequest() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header("Origin", FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test
    @DisplayName("un origen desconocido es rechazado")
    void rejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "https://sitio-malicioso.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
