package com.frontpet.booking;

import com.frontpet.AbstractIntegrationTest;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link AppointmentRateLimitFilter} sobre el stack HTTP real (tarea 5.8).
 * Vive en {@code com.frontpet.booking} y no en {@code .api} por la misma
 * razón que {@code OrderRateLimitIntegrationTest}: necesita el
 * {@code clear()} package-private de {@link AppointmentRateLimitService}.
 *
 * <p>Sin {@code @Transactional} a propósito: el rate limit vive en un filtro
 * fuera de la transacción HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentRateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired ServicePricingRepository servicePricingRepository;
    @Autowired AppointmentRateLimitService rateLimit;
    @Autowired AppointmentRateLimitProperties props;
    @Value("${frontpet.tenant.id}") UUID tenantId;

    private Long banhoBaseId;

    @BeforeEach
    void setUp() {
        rateLimit.clear();

        ServiceOffering banhoBase = new ServiceOffering();
        banhoBase.setTenantId(tenantId);
        banhoBase.setType(ServiceType.BASE);
        banhoBase.setNome("Banho Rate Limit Teste " + UUID.randomUUID());
        serviceOfferingRepository.saveAndFlush(banhoBase);

        ServicePricing pricing = new ServicePricing();
        pricing.setService(banhoBase);
        pricing.setSize(Porte.M);
        pricing.setPrice(new BigDecimal("59.00"));
        pricing.setDurationMinutes(60);
        servicePricingRepository.saveAndFlush(pricing);

        banhoBaseId = banhoBase.getId();
    }

    @Test
    @DisplayName("al superar maxRequests desde una IP, la siguiente reserva da 429 com Retry-After")
    void exceedingMaxRequestsBlocksTheIp() throws Exception {
        String ip = "203.0.113.40";
        // Cada request cuenta aunque el resultado sea 400/409 (mismo criterio
        // que OrderRateLimitService: no hay "éxito" que resetee el contador),
        // así que un horário inválido alcanza para no ensuciar la agenda real.
        for (int i = 0; i < props.maxRequests(); i++) {
            mockMvc.perform(appointmentFrom(ip)).andExpect(status().isBadRequest());
        }

        mockMvc.perform(appointmentFrom(ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("otra IP no comparte el contador")
    void differentIpHasItsOwnBudget() throws Exception {
        String ip = "203.0.113.41";
        for (int i = 0; i < props.maxRequests(); i++) {
            mockMvc.perform(appointmentFrom(ip)).andExpect(status().isBadRequest());
        }

        mockMvc.perform(appointmentFrom("203.0.113.42")).andExpect(status().isBadRequest());
    }

    /** Horário fuera de la grilla de 30 min: 400 rápido, sin tocar la agenda real. */
    private MockHttpServletRequestBuilder appointmentFrom(String ip) {
        String body = """
                {
                  "baseServiceId": %d,
                  "porte": "M",
                  "data": "%s",
                  "horario": "10:15",
                  "clienteNome": "Rate Limit Test",
                  "clienteTelefone": "51999998888",
                  "petNome": "Thor"
                }
                """.formatted(banhoBaseId, proximaQuarta());

        return post("/api/v1/appointments")
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

    private LocalDate proximaQuarta() {
        LocalDate data = LocalDate.now(SlotGrid.ZONE_ID).plusDays(7);
        while (data.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            data = data.plusDays(1);
        }
        return data;
    }
}
