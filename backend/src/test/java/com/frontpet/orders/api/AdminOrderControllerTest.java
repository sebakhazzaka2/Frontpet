package com.frontpet.orders.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.common.UuidV7;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.orders.OrderService;
import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.ModalidadeEntrega;
import com.frontpet.orders.dto.CreateOrderItemRequest;
import com.frontpet.orders.dto.CreateOrderRequest;
import com.frontpet.orders.dto.OrderDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestão de pedidos del admin, sobre el stack HTTP real. Mismo patrón que
 * {@code AdminProductControllerTest}: {@code AdminUser} en memoria, sin login
 * real ni fila en {@code admin_users}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOrderControllerTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired OrderService orderService;

    private final AdminUser admin = adminFor(TENANT);

    @Test
    @DisplayName("GET sin cookie JWT devuelve 401")
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET devuelve el listado paginado y filtra por status")
    void listsAndFiltersByStatus() throws Exception {
        Product product = persistProduct("Produto Admin Orders", "produto-admin-orders", new BigDecimal("20.00"));
        OrderDetail created = createOrder(product.getPublicId());
        orderService.updateStatus(TENANT, created.publicId(), com.frontpet.orders.domain.OrderStatus.CONFIRMED);

        mockMvc.perform(get("/api/v1/admin/orders")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "CONFIRMED")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.publicId == '" + created.publicId() + "')]").exists());

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "CANCELLED")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.publicId == '" + created.publicId() + "')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /counts devuelve los contadores por status")
    void returnsCounts() throws Exception {
        Product product = persistProduct("Produto Admin Counts", "produto-admin-counts", new BigDecimal("20.00"));
        createOrder(product.getPublicId());

        mockMvc.perform(get("/api/v1/admin/orders/counts")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendentes").isNumber())
                .andExpect(jsonPath("$.todos").isNumber());
    }

    @Test
    @DisplayName("GET /{publicId} devuelve el detalle")
    void getsDetail() throws Exception {
        Product product = persistProduct("Produto Admin Detalhe", "produto-admin-detalhe", new BigDecimal("20.00"));
        OrderDetail created = createOrder(product.getPublicId());

        mockMvc.perform(get("/api/v1/admin/orders/" + created.publicId())
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(created.publicId().toString()));
    }

    @Test
    @DisplayName("pedido de outro tenant da 404")
    void otherTenantOrderIsNotFound() throws Exception {
        // El pedido se crea normalmente bajo TENANT (products/orders tienen
        // FK real a tenants — no se puede insertar bajo un tenant que no
        // existe). Lo que se prueba es que la query por tenantId filtra de
        // verdad: un AdminUser de OTRO tenant (nunca persistido, sin FK) no
        // encuentra el pedido aunque el publicId sea válido.
        Product product = persistProduct("Produto Outro Tenant", "produto-outro-tenant", new BigDecimal("10.00"));
        OrderDetail order = createOrder(product.getPublicId());
        AdminUser adminOtherTenant = adminFor(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/admin/orders/" + order.publicId())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminOtherTenant)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH status muda o pedido e sela confirmedAt")
    void patchesStatus() throws Exception {
        Product product = persistProduct("Produto Admin Patch", "produto-admin-patch", new BigDecimal("20.00"));
        OrderDetail created = createOrder(product.getPublicId());

        mockMvc.perform(patch("/api/v1/admin/orders/" + created.publicId() + "/status")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());
    }

    // ---- helpers -------------------------------------------------------

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

    private OrderDetail createOrder(UUID productPublicId) {
        return orderService.create(TENANT, new CreateOrderRequest(
                "Cliente Admin Test", "51999998888", ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(productPublicId, null, 1))));
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
