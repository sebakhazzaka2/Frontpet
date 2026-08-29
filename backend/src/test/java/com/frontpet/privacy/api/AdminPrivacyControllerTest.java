package com.frontpet.privacy.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.domain.Appointment;
import com.frontpet.booking.domain.AppointmentRepository;
import com.frontpet.booking.domain.AppointmentStatus;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServiceType;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.common.PhoneNormalizer;
import com.frontpet.common.UuidV7;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.orders.OrderService;
import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.ModalidadeEntrega;
import com.frontpet.orders.domain.Order;
import com.frontpet.orders.domain.OrderRepository;
import com.frontpet.orders.dto.CreateOrderItemRequest;
import com.frontpet.orders.dto.CreateOrderRequest;
import com.frontpet.orders.dto.OrderDetail;
import com.frontpet.privacy.domain.PrivacyErasureLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Direito de eliminação LGPD (tarea 7.14, ADR 023) sobre o stack HTTP real.
 * Mesmo padrão de {@code AdminOrderControllerTest}: {@code AdminUser} em
 * memória, sem login real nem fila em {@code admin_users}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPrivacyControllerTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");
    private static final String TELEFONE = "51999998888";

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired OrderService orderService;
    @Autowired PrivacyErasureLogRepository privacyErasureLogRepository;

    private final AdminUser admin = adminFor(TENANT);

    @Test
    @DisplayName("GET /preview sin cookie devuelve 401")
    void previewRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/privacy/preview").param("telefone", TELEFONE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /preview lista os pedidos e turnos do titular")
    void previewListsOrdersAndAppointments() throws Exception {
        Order order = criarPedido();
        Appointment appointment = criarTurno();

        mockMvc.perform(get("/api/v1/admin/privacy/preview")
                        .param("telefone", TELEFONE)
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderPublicIds[0]").value(order.getPublicId().toString()))
                .andExpect(jsonPath("$.appointmentPublicIds[0]").value(appointment.getPublicId().toString()));
    }

    @Test
    @DisplayName("GET /preview sem nenhum registro devuelve 404")
    void previewWithoutRecordsReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/privacy/preview")
                        .param("telefone", "51900000000")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("titular de outro tenant não aparece no preview (404)")
    void otherTenantRecordsAreNotFound() throws Exception {
        criarPedido();
        criarTurno();
        AdminUser adminOtherTenant = adminFor(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/admin/privacy/preview")
                        .param("telefone", TELEFONE)
                        .with(SecurityMockMvcRequestPostProcessors.user(adminOtherTenant)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /anonymize limpa dados pessoais e preserva snapshots contábeis")
    void anonymizePreservesFinancialSnapshots() throws Exception {
        Order order = criarPedido();
        Appointment appointment = criarTurno();

        mockMvc.perform(post("/api/v1/admin/privacy/anonymize")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"clienteTelefone\": \"" + TELEFONE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersAnonymized").value(1))
                .andExpect(jsonPath("$.appointmentsAnonymized").value(1));

        Order orderAnonimizado = orderRepository.findByTenantIdAndPublicId(TENANT, order.getPublicId()).orElseThrow();
        assertThat(orderAnonimizado.getClienteNome()).isEqualTo("Titular removido");
        assertThat(orderAnonimizado.getClienteTelefoneNorm()).isNull();
        assertThat(orderAnonimizado.getAnonymizedAt()).isNotNull();
        // Dado contábil intacto (ADR 023): o item e o subtotal não mudam.
        assertThat(orderAnonimizado.getSubtotalSnapshot()).isEqualByComparingTo("20.00");
        assertThat(orderAnonimizado.getItems()).hasSize(1);

        Appointment turnoAnonimizado =
                appointmentRepository.findByTenantIdAndPublicId(TENANT, appointment.getPublicId()).orElseThrow();
        assertThat(turnoAnonimizado.getClienteNome()).isEqualTo("Titular removido");
        assertThat(turnoAnonimizado.getPetNome()).isEqualTo("—");
        assertThat(turnoAnonimizado.getClienteTelefoneNorm()).isNull();
        assertThat(turnoAnonimizado.getAnonymizedAt()).isNotNull();
        // Snapshot de preço/duração intacto (ADR 023).
        assertThat(turnoAnonimizado.getTotalPriceSnapshot()).isEqualByComparingTo("59.00");

        assertThat(privacyErasureLogRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("uma segunda chamada de anonymize devuelve 404 (idempotente)")
    void secondAnonymizeCallReturns404() throws Exception {
        criarPedido();

        mockMvc.perform(post("/api/v1/admin/privacy/anonymize")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"clienteTelefone\": \"" + TELEFONE + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/privacy/anonymize")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content("{\"clienteTelefone\": \"" + TELEFONE + "\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- helpers -------------------------------------------------------

    private Order criarPedido() {
        Product product = new Product();
        product.setPublicId(UuidV7.generate());
        product.setTenantId(TENANT);
        product.setNome("Produto Privacy Test");
        product.setSlug("produto-privacy-test-" + UUID.randomUUID());
        product.setPrice(new BigDecimal("20.00"));
        product.setStock(10);
        product.setActive(true);
        Product saved = productRepository.saveAndFlush(product);

        OrderDetail detail = orderService.create(TENANT, new CreateOrderRequest(
                "Cliente Privacy Test", TELEFONE, ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(saved.getPublicId(), null, 1))));

        return orderRepository.findByTenantIdAndPublicId(TENANT, detail.publicId()).orElseThrow();
    }

    private Appointment criarTurno() {
        ServiceOffering banhoBase = new ServiceOffering();
        banhoBase.setTenantId(TENANT);
        banhoBase.setType(ServiceType.BASE);
        banhoBase.setNome("Banho Privacy Test");
        serviceOfferingRepository.save(banhoBase);

        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS);

        Appointment appointment = new Appointment();
        appointment.setPublicId(UuidV7.generate());
        appointment.setTenantId(TENANT);
        appointment.setBaseService(banhoBase);
        appointment.setSize(com.frontpet.booking.domain.Porte.M);
        appointment.setStartAt(inicio);
        appointment.setEndAt(inicio.plus(60, ChronoUnit.MINUTES));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setClienteNome("Cliente Privacy Test");
        appointment.setClienteTelefone(TELEFONE);
        appointment.setClienteTelefoneNorm(PhoneNormalizer.normalizeBr(TELEFONE));
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
        user.setEmail("admin-test@frontpet.com.br");
        user.setPasswordHash("{bcrypt}$2a$10$fakehashfortestingonly");
        user.setRole("ADMIN");
        return user;
    }
}
