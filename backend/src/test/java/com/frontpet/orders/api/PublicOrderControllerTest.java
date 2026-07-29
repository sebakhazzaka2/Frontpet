package com.frontpet.orders.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.common.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checkout público sobre el stack HTTP real. Desactiva el rate limit de
 * orders (varios POSTs por test) — se cubre aparte en
 * {@code OrderRateLimitIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "frontpet.order-rate-limit.enabled=false")
class PublicOrderControllerTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;

    @Test
    @DisplayName("POST /api/v1/orders sin cookie devuelve 201 (checkout es anónimo)")
    void createsOrderWithoutAuthentication() throws Exception {
        Product product = persistProduct("Ração Checkout Público", "racao-checkout-publico", new BigDecimal("45.90"));

        String body = """
                {
                  "clienteNome": "Maria Souza",
                  "clienteTelefone": "(51) 99999-8888",
                  "modalidade": "RETIRADA",
                  "formaPagamento": "PIX",
                  "consentimentoLgpd": true,
                  "items": [{"productPublicId": "%s", "quantidade": 1}]
                }
                """.formatted(product.getPublicId());

        mockMvc.perform(post("/api/v1/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].nomeSnapshot").value("Ração Checkout Público"));
    }

    @Test
    @DisplayName("consentimentoLgpd=false da 400 (viene del @AssertTrue del DTO)")
    void rejectsWithoutLgpdConsent() throws Exception {
        Product product = persistProduct("Ração Sem Consentimento", "racao-sem-consentimento", new BigDecimal("45.90"));

        String body = """
                {
                  "clienteNome": "Maria Souza",
                  "clienteTelefone": "(51) 99999-8888",
                  "modalidade": "RETIRADA",
                  "formaPagamento": "PIX",
                  "consentimentoLgpd": false,
                  "items": [{"productPublicId": "%s", "quantidade": 1}]
                }
                """.formatted(product.getPublicId());

        mockMvc.perform(post("/api/v1/orders").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("clienteNome vacío da 400 con fieldErrors")
    void rejectsBlankClientName() throws Exception {
        String body = """
                {
                  "clienteNome": "",
                  "clienteTelefone": "(51) 99999-8888",
                  "modalidade": "RETIRADA",
                  "formaPagamento": "PIX",
                  "consentimentoLgpd": true,
                  "items": [{"productPublicId": "%s", "quantidade": 1}]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.clienteNome").exists());
    }

    @Test
    @DisplayName("items vacío da 400")
    void rejectsEmptyItems() throws Exception {
        String body = """
                {
                  "clienteNome": "Maria Souza",
                  "clienteTelefone": "(51) 99999-8888",
                  "modalidade": "RETIRADA",
                  "formaPagamento": "PIX",
                  "consentimentoLgpd": true,
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    private Product persistProduct(String nome, String slug, BigDecimal price) {
        Product p = new Product();
        p.setPublicId(UuidV7.generate());
        p.setTenantId(TENANT);
        p.setNome(nome);
        p.setSlug(slug);
        p.setPrice(price);
        p.setStock(10);
        p.setActive(true);
        return productRepository.saveAndFlush(p);
    }
}
