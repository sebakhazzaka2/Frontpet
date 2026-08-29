package com.frontpet.identity;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.identity.domain.AdminUserRepository;
import com.frontpet.notifications.EmailMessage;
import com.frontpet.notifications.EmailSender;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link PasswordResetRateLimitFilter} (IP) y el límite por email de
 * {@link PasswordResetEmailAttemptService} sobre el stack HTTP real (tarea
 * 7.12). Vive en {@code com.frontpet.identity} y no en {@code .api} por la
 * misma razón que {@code AuthControllerRateLimitIntegrationTest}: necesita el
 * {@code clear()} package-private de ambos servicios.
 *
 * <p>Sin {@code @Transactional} a propósito, mismo criterio que
 * {@code OrderRateLimitIntegrationTest}: el rate limit vive en memoria, no en
 * la DB, y no depende de que el request corra dentro de una transacción de
 * test que después se revierte.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PasswordResetRateLimitIntegrationTest.TestEmailConfig.class)
class PasswordResetRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String FORGOT_URL = "/api/v1/auth/forgot-password";
    private static final String RESET_URL = "/api/v1/auth/reset-password";

    @Value("${frontpet.tenant.id}")
    private UUID tenantId;

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetIpAttemptService ipAttempts;
    @Autowired private PasswordResetEmailAttemptService emailAttempts;
    @Autowired private PasswordResetProperties props;
    @Autowired private CapturingEmailSender emailSender;

    @BeforeEach
    void clearState() {
        ipAttempts.clear();
        emailAttempts.clear();
        emailSender.clear();
    }

    @Test
    @DisplayName("al superar el límite por IP, el siguiente forgot-password da 429 con Retry-After")
    void exceedingIpLimitBlocksForgotPassword() throws Exception {
        String ip = "203.0.113.50";
        for (int i = 0; i < props.ipRateLimit().maxAttempts(); i++) {
            mockMvc.perform(forgotPasswordFrom(ip, "qualquer-" + i + "@frontpet.dev"))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(forgotPasswordFrom(ip, "mais-um@frontpet.dev"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("el límite por IP también cubre reset-password")
    void ipLimitAlsoCoversResetPassword() throws Exception {
        String ip = "203.0.113.51";
        for (int i = 0; i < props.ipRateLimit().maxAttempts(); i++) {
            mockMvc.perform(resetPasswordFrom(ip, "token-invalido-" + i, "nova-senha-123"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(resetPasswordFrom(ip, "token-invalido-extra", "nova-senha-123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("otra IP no comparte el contador")
    void differentIpHasItsOwnBudget() throws Exception {
        String ip = "203.0.113.52";
        for (int i = 0; i < props.ipRateLimit().maxAttempts(); i++) {
            mockMvc.perform(forgotPasswordFrom(ip, "qualquer-" + i + "@frontpet.dev"))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(forgotPasswordFrom("203.0.113.53", "outro@frontpet.dev"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("superar el límite por email sigue devolviendo 202 — solo se envían los primeros mails")
    void exceedingEmailLimitStillReturns202ButStopsSending() throws Exception {
        AdminUser user = createTestAdmin("mesmo-email-" + UUID.randomUUID() + "@frontpet.dev");
        int maxByEmail = props.emailRateLimit().maxAttempts();

        // Desde IPs distintas a propósito: lo que se prueba acá es el límite
        // por EMAIL (que vive en PasswordResetService, no en el filtro), no
        // el de IP, que ya tiene su propia cobertura arriba.
        for (int i = 0; i < maxByEmail + 2; i++) {
            mockMvc.perform(forgotPasswordFrom("203.0.113.6" + i, user.getEmail()))
                    .andExpect(status().isAccepted());
        }

        assertThat(emailSender.sent).hasSize(maxByEmail);
    }

    private AdminUser createTestAdmin(String email) {
        AdminUser user = new AdminUser();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.changePassword(passwordEncoder.encode("senha-original-123"), Instant.now().minusSeconds(5));
        user.setRole("ADMIN");
        return adminUserRepository.saveAndFlush(user);
    }

    private MockHttpServletRequestBuilder forgotPasswordFrom(String ip, String email) {
        return post(FORGOT_URL)
                .with(fromIp(ip))
                .contentType("application/json")
                .content("""
                        {"email": "%s"}
                        """.formatted(email));
    }

    private MockHttpServletRequestBuilder resetPasswordFrom(String ip, String token, String password) {
        return post(RESET_URL)
                .with(fromIp(ip))
                .contentType("application/json")
                .content("""
                        {"token": "%s", "password": "%s"}
                        """.formatted(token, password));
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @TestConfiguration
    static class TestEmailConfig {
        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    /** Cuenta los emails "enviados" en vez de mandarlos de verdad — reemplaza al EmailSender real vía @Primary. */
    static class CapturingEmailSender implements EmailSender {
        private final List<EmailMessage> sent = new ArrayList<>();

        @Override
        public void send(EmailMessage message) {
            sent.add(message);
        }

        void clear() {
            sent.clear();
        }
    }
}
