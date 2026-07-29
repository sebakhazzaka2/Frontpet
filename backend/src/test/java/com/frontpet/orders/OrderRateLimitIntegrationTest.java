package com.frontpet.orders;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.common.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link OrderRateLimitFilter} sobre el stack HTTP real (tarea 4.15). Vive en
 * {@code com.frontpet.orders} y no en {@code .api} por la misma razón que
 * {@code identity.AuthControllerRateLimitIntegrationTest}: necesita el
 * {@code clear()} package-private de {@link OrderRateLimitService}.
 *
 * <p>Sin {@code @Transactional} a propósito: el rate limit vive en un filtro
 * fuera de la transacción HTTP, así que cada POST debe llegar de verdad al
 * filtro y persistir (o no) según corresponda.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRateLimitService rateLimit;
    @Autowired OrderRateLimitProperties props;

    private Product product;

    @BeforeEach
    void setUp() {
        rateLimit.clear();
        product = productRepository.saveAndFlush(newProduct());
    }

    @Test
    @DisplayName("al superar maxRequests desde una IP, el siguiente pedido da 429 com Retry-After")
    void exceedingMaxRequestsBlocksTheIp() throws Exception {
        String ip = "203.0.113.30";
        for (int i = 0; i < props.maxRequests(); i++) {
            mockMvc.perform(orderFrom(ip)).andExpect(status().isCreated());
        }

        mockMvc.perform(orderFrom(ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("otra IP no comparte el contador")
    void differentIpHasItsOwnBudget() throws Exception {
        String ip = "203.0.113.31";
        for (int i = 0; i < props.maxRequests(); i++) {
            mockMvc.perform(orderFrom(ip)).andExpect(status().isCreated());
        }

        mockMvc.perform(orderFrom("203.0.113.32")).andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder orderFrom(String ip) {
        String body = """
                {
                  "clienteNome": "Rate Limit Test",
                  "clienteTelefone": "51999998888",
                  "modalidade": "RETIRADA",
                  "formaPagamento": "PIX",
                  "consentimentoLgpd": true,
                  "items": [{"productPublicId": "%s", "quantidade": 1}]
                }
                """.formatted(product.getPublicId());

        return post("/api/v1/orders")
                .with(fromIp(ip))
                .contentType("application/json")
                .content(body);
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private Product newProduct() {
        Product p = new Product();
        p.setPublicId(UuidV7.generate());
        p.setTenantId(TENANT);
        p.setNome("Produto Rate Limit Orders");
        p.setSlug("produto-rate-limit-orders-" + UuidV7.generate());
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(10);
        p.setActive(true);
        return p;
    }
}
