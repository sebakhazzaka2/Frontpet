package com.frontpet.booking.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/services} (tarea 5.4). El tenant sale de
 * {@code frontpet.tenant.id} (single-tenant MVP1), no de un tenant propio de
 * test: por eso reusa el ID sembrado en V5, único con el que
 * {@link com.frontpet.tenant.CurrentTenant} resuelve algo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicServiceControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Value("${frontpet.tenant.id}") UUID tenantId;

    @BeforeEach
    void seed() {
        ServiceOffering ativo = new ServiceOffering();
        ativo.setTenantId(tenantId);
        ativo.setType(ServiceType.BASE);
        ativo.setNome("Banho Público Ativo");
        ativo.setActive(true);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(ativo);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        // Agregado a la colección del padre, no guardado por separado: mismo
        // motivo que AdminServiceControllerTest#setUp — @Transactional
        // comparte la persistence context entre @BeforeEach y el test, y un
        // save() aislado del hijo dejaría en memoria la colección de ativo
        // vacía aunque el registro exista en DB.
        ativo.getPricing().add(pricing);
        serviceOfferingRepository.save(ativo);

        ServiceOffering inativo = new ServiceOffering();
        inativo.setTenantId(tenantId);
        inativo.setType(ServiceType.BASE);
        inativo.setNome("Banho Público Inativo");
        inativo.setActive(false);
        serviceOfferingRepository.save(inativo);
    }

    @Test
    @DisplayName("GET /services sin autenticação devuelve 200 con solo os ativos")
    void listsOnlyActiveServices() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome == 'Banho Público Ativo')]").exists())
                .andExpect(jsonPath("$[?(@.nome == 'Banho Público Inativo')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.nome == 'Banho Público Ativo')].pricing[0].price").value(59.00));
    }
}
