package com.frontpet.booking.api;

import com.frontpet.AbstractIntegrationTest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
 * Admin de serviços (tarea 5.7, issue #51, Bloque G) sobre el stack HTTP
 * real. Mismo patrón que {@code AdminOrderControllerTest}: {@code AdminUser}
 * en memoria, sin login real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminServiceControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TenantRepository tenantRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;

    private UUID tenantId;
    private AdminUser admin;
    private ServiceOffering baseService;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setId(UuidV7.generate());
        tenant.setNome("Tenant Admin Services");
        tenant.setWhatsappDestino("+5555999990000");
        tenant.setConfig(Map.of());
        tenantRepository.save(tenant);
        tenantId = tenant.getId();
        admin = adminFor(tenantId);

        baseService = new ServiceOffering();
        baseService.setTenantId(tenantId);
        baseService.setType(ServiceType.BASE);
        baseService.setNome("Banho Essencial");
        baseService.setDescricao("Descrição original.");

        ServicePricing pricingP = new ServicePricing();
        pricingP.setService(baseService);
        pricingP.setSize(Porte.P);
        pricingP.setPrice(new BigDecimal("49.00"));
        pricingP.setDurationMinutes(45);
        // Agregado a la colección del padre (no guardado por separado): el
        // cascade=ALL de ServiceOffering.pricing lo persiste, y así el
        // objeto en memoria de baseService ya trae el hijo — necesario
        // porque @Transactional comparte la persistence context entre este
        // @BeforeEach y el test, y un save() aislado del hijo dejaría la
        // colección en memoria de baseService vacía (misma instancia Java,
        // sin recargar de la DB) aunque el registro sí exista.
        baseService.getPricing().add(pricingP);
        serviceOfferingRepository.save(baseService);
    }

    @Test
    @DisplayName("PUT sin cookie JWT devuelve 401")
    void updateRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/admin/services/" + baseService.getId())
                        .contentType("application/json")
                        .content(validUpdateBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET sin cookie JWT devuelve 401")
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET devuelve todos los serviços, incluidos los inactivos")
    void listsAllServicesIncludingInactive() throws Exception {
        ServiceOffering inactiveService = new ServiceOffering();
        inactiveService.setTenantId(tenantId);
        inactiveService.setType(ServiceType.ADDON);
        inactiveService.setNome("Hidratação");
        inactiveService.setDescricao("Descrição.");
        inactiveService.setActive(false);
        serviceOfferingRepository.save(inactiveService);

        mockMvc.perform(get("/api/v1/admin/services")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[?(@.active == false)]", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("PUT edita nome/descricao/active y tarifa por porte")
    void updatesServiceAndPricing() throws Exception {
        mockMvc.perform(put("/api/v1/admin/services/" + baseService.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(validUpdateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Banho Essencial Editado"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.pricing[0].price").value(52.00));

        ServicePricing updated = servicePricingRepository.findByServiceIdAndSize(baseService.getId(), Porte.P)
                .orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("52.00");
        assertThat(updated.getDurationMinutes()).isEqualTo(50);
    }

    @Test
    @DisplayName("PUT con porte que el serviço não tem sembrado devuelve 400")
    void updateWithUnknownPorteReturnsBadRequest() throws Exception {
        String body = """
                {
                  "nome": "Banho Essencial",
                  "descricao": "Descrição.",
                  "active": true,
                  "pricing": [ { "size": "GG", "price": 95.00, "durationMinutes": 120 } ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/services/" + baseService.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT sobre serviço de outro tenant da 404")
    void updateOtherTenantServiceIsNotFound() throws Exception {
        AdminUser adminOtherTenant = adminFor(UUID.randomUUID());

        mockMvc.perform(put("/api/v1/admin/services/" + baseService.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(adminOtherTenant))
                        .contentType("application/json")
                        .content(validUpdateBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /admin/services/{id} devuelve 405 (ADR 009 — admin não cria serviços)")
    void postIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/admin/services/" + baseService.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(admin))
                        .contentType("application/json")
                        .content(validUpdateBody()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("DELETE /admin/services/{id} devuelve 405 (ADR 009 — admin não apaga serviços)")
    void deleteIsNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/services/" + baseService.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isMethodNotAllowed());
    }

    private String validUpdateBody() {
        return """
                {
                  "nome": "Banho Essencial Editado",
                  "descricao": "Descrição editada.",
                  "active": false,
                  "pricing": [ { "size": "P", "price": 52.00, "durationMinutes": 50 } ]
                }
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
