package com.frontpet.booking.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.booking.SlotGrid;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.ServicePricingRepository;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/availability} (tarea 5.4) sobre el stack HTTP real. La
 * lógica del cálculo ya está a fondo en {@code AvailabilityServiceIntegrationTest}
 * — acá solo se verifica el wiring HTTP: sin autenticación, binding de query
 * params, y el shape de la respuesta.
 *
 * <p>Usa el tenant fijo de {@code frontpet.tenant.id} (V5, Seg-Sex 09:00-17:00)
 * en vez de uno propio: {@code business_hours} tiene UNIQUE(tenant_id, dia)
 * y ya viene sembrado para los 7 días.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicAvailabilityControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Value("${frontpet.tenant.id}") UUID tenantId;

    private ServiceOffering banhoBase;

    @BeforeEach
    void seed() {
        banhoBase = new ServiceOffering();
        banhoBase.setTenantId(tenantId);
        banhoBase.setType(ServiceType.BASE);
        banhoBase.setNome("Banho HTTP Teste");
        serviceOfferingRepository.save(banhoBase);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(banhoBase);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        servicePricingRepository.save(pricing);
    }

    @Test
    @DisplayName("GET /availability sin autenticação devuelve 200 con la grilla del día")
    void returnsAvailabilityWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/availability")
                        .param("baseServiceId", banhoBase.getId().toString())
                        .param("porte", "M")
                        .param("data", proximaQuarta().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracaoTotalMinutes").value(60))
                .andExpect(jsonPath("$.precoTotal").value(59.00))
                .andExpect(jsonPath("$.indisponibilidade").doesNotExist())
                .andExpect(jsonPath("$.slots[0].horario").value("09:00"));
    }

    @Test
    @DisplayName("baseServiceId de un tenant que no tiene ese id devuelve 400")
    void unknownServiceReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/availability")
                        .param("baseServiceId", "999999")
                        .param("porte", "M")
                        .param("data", proximaQuarta().toString()))
                .andExpect(status().isBadRequest());
    }

    private LocalDate proximaQuarta() {
        LocalDate data = LocalDate.now(SlotGrid.ZONE_ID).plusDays(7);
        while (data.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            data = data.plusDays(1);
        }
        return data;
    }
}
