package com.frontpet.identity.api;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.identity.domain.AdminUser;
import com.frontpet.identity.domain.AdminUserRepository;
import com.frontpet.notifications.EmailMessage;
import com.frontpet.notifications.EmailSender;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ida y vuelta real de {@code /forgot-password} + {@code /reset-password}
 * (tarea 7.12) sobre el stack HTTP completo. Desactiva los rate limits del
 * reset (varios POSTs por test) — se cubren aparte en
 * {@code PasswordResetRateLimitIntegrationTest}, mismo criterio que
 * {@code PublicOrderControllerTest}/{@code OrderRateLimitIntegrationTest}.
 *
 * <p>Cada test crea su propio admin con email único (en vez de reusar el
 * admin sembrado por {@code DataInitializer}): así una senha nueva escrita
 * acá nunca puede filtrarse a {@code AuthControllerIntegrationTest}, que
 * loguea con las credenciales de config. {@code @Transactional} igual
 * garantiza el rollback al final de cada test.
 *
 * <p>{@link CapturingEmailSender} reemplaza al {@code EmailSender} real vía
 * {@code @Primary}: sin {@code RESEND_API_KEY} en tests, el bean real sería
 * {@code LoggingEmailSender} (solo loguea) — acá hace falta leer el token del
 * link para completar el flujo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(PasswordResetControllerIntegrationTest.TestEmailConfig.class)
@TestPropertySource(properties = {
        "frontpet.password-reset.ip-rate-limit.enabled=false",
        "frontpet.password-reset.email-rate-limit.enabled=false"
})
class PasswordResetControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String FORGOT_URL = "/api/v1/auth/forgot-password";
    private static final String RESET_URL = "/api/v1/auth/reset-password";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PROTECTED_URL = "/api/v1/admin/products/images/presign";
    private static final String VALID_PRESIGN_BODY = """
            {"fileName": "foto.png", "contentType": "image/png", "contentLength": 500000}
            """;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    private static final String ORIGINAL_PASSWORD = "senha-original-123";

    @Value("${frontpet.tenant.id}")
    private UUID tenantId;

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CapturingEmailSender emailSender;

    @BeforeEach
    void clearCapturedEmails() {
        emailSender.clear();
    }

    @Test
    @DisplayName("forgot-password con email conocido devuelve 202")
    void forgotPasswordWithKnownEmailReturns202() throws Exception {
        AdminUser user = createTestAdmin("known-" + UUID.randomUUID() + "@frontpet.dev");

        mockMvc.perform(forgotPassword(user.getEmail())).andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("forgot-password con email desconocido devuelve el mismo 202 (anti-enumeração)")
    void forgotPasswordWithUnknownEmailReturnsSame202() throws Exception {
        mockMvc.perform(forgotPassword("nao-existe-" + UUID.randomUUID() + "@frontpet.dev"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("forgot-password con email de formato inválido devuelve 400 con fieldErrors")
    void forgotPasswordWithInvalidEmailFormatReturns400() throws Exception {
        mockMvc.perform(forgotPassword("isto-nao-e-um-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("flujo completo: forgot -> extraer token -> reset -> login con la senha nueva da 200 + Set-Cookie")
    void fullResetFlowAllowsLoginWithNewPassword() throws Exception {
        AdminUser user = createTestAdmin("full-flow-" + UUID.randomUUID() + "@frontpet.dev");

        mockMvc.perform(forgotPassword(user.getEmail())).andExpect(status().isAccepted());
        String token = extractLastToken();

        mockMvc.perform(resetPassword(token, "nova-senha-123")).andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(login(user.getEmail(), "nova-senha-123"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(loginResult.getResponse().getCookie("frontpet_session")).isNotNull();
    }

    @Test
    @DisplayName("después del reset, la senha vieja devuelve 401")
    void oldPasswordFailsAfterReset() throws Exception {
        AdminUser user = createTestAdmin("old-password-" + UUID.randomUUID() + "@frontpet.dev");

        mockMvc.perform(forgotPassword(user.getEmail())).andExpect(status().isAccepted());
        String token = extractLastToken();
        mockMvc.perform(resetPassword(token, "nova-senha-123")).andExpect(status().isOk());

        mockMvc.perform(login(user.getEmail(), ORIGINAL_PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token inventado y token ya usado devuelven 400 con el mismo mensaje")
    void invalidAndUsedTokensReturnSameMessage() throws Exception {
        AdminUser user = createTestAdmin("used-token-" + UUID.randomUUID() + "@frontpet.dev");

        mockMvc.perform(resetPassword("token-que-nao-existe", "nova-senha-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Link de redefinição inválido ou expirado."));

        mockMvc.perform(forgotPassword(user.getEmail())).andExpect(status().isAccepted());
        String token = extractLastToken();
        mockMvc.perform(resetPassword(token, "nova-senha-123")).andExpect(status().isOk());

        // El mismo token, usado por segunda vez.
        mockMvc.perform(resetPassword(token, "outra-senha-456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Link de redefinição inválido ou expirado."));
    }

    @Test
    @DisplayName("senha de menos de 8 caracteres devuelve 400 con fieldErrors.password")
    void passwordShorterThan8CharsReturns400() throws Exception {
        mockMvc.perform(resetPassword("qualquer-token", "curta12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("reset-password devuelve Set-Cookie con Max-Age=0")
    void resetPasswordClearsSessionCookie() throws Exception {
        AdminUser user = createTestAdmin("clear-cookie-" + UUID.randomUUID() + "@frontpet.dev");
        mockMvc.perform(forgotPassword(user.getEmail())).andExpect(status().isAccepted());
        String token = extractLastToken();

        MvcResult result = mockMvc.perform(resetPassword(token, "nova-senha-123"))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("Max-Age=0");
    }

    /**
     * El test más importante del bloque: sin la invalidación de sesión de
     * {@code JwtAuthFilter} (ver {@code JwtAuthFilterTest}), un reset exitoso
     * no tendría ningún efecto sobre una cookie ya emitida. No hace falta
     * mockear un {@code Clock} acá — {@code sessionNotRevoked} redondea
     * {@code floor(passwordChangedAt) + 1s}, así que CUALQUIER reset que
     * ocurra después del login (incluso dentro del mismo segundo de
     * wall-clock, el caso límite que {@code JwtAuthFilterTest} documenta con
     * instantes controlados) invalida la sesión — es justamente el propósito
     * de ese +1.
     */
    @Test
    @DisplayName("un reset exitoso invalida la sesión preexistente de ese admin")
    void resetInvalidatesPreexistingSession() throws Exception {
        AdminUser user = createTestAdmin("invalida-sessao-" + UUID.randomUUID() + "@frontpet.dev");

        Cookie sessionCookie = mockMvc.perform(login(user.getEmail(), ORIGINAL_PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("frontpet_session");
        assertThat(sessionCookie).isNotNull();

        mockMvc.perform(post(PROTECTED_URL)
                        .cookie(sessionCookie)
                        .contentType("application/json")
                        .content(VALID_PRESIGN_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(forgotPassword(user.getEmail())).andExpect(status().isAccepted());
        String token = extractLastToken();
        mockMvc.perform(resetPassword(token, "nova-senha-123")).andExpect(status().isOk());

        mockMvc.perform(post(PROTECTED_URL)
                        .cookie(sessionCookie)
                        .contentType("application/json")
                        .content(VALID_PRESIGN_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ------------------------------------------------------

    private AdminUser createTestAdmin(String email) {
        AdminUser user = new AdminUser();
        user.setTenantId(tenantId);
        user.setEmail(email);
        // passwordChangedAt unos segundos en el pasado, no Instant.now(): si
        // quedara pegado al mismo segundo del primer login, JwtAuthFilter lo
        // invalidaría por el redondeo de floor()+1s antes de que exista
        // ningún reset real que probar (ver el javadoc de sessionNotRevoked).
        user.changePassword(passwordEncoder.encode(ORIGINAL_PASSWORD), Instant.now().minusSeconds(5));
        user.setRole("ADMIN");
        return adminUserRepository.saveAndFlush(user);
    }

    private String extractLastToken() {
        EmailMessage last = emailSender.sent.get(emailSender.sent.size() - 1);
        Matcher matcher = TOKEN_PATTERN.matcher(last.htmlBody());
        if (!matcher.find()) {
            throw new IllegalStateException("Link de reset não encontrado no e-mail capturado.");
        }
        return matcher.group(1);
    }

    private MockHttpServletRequestBuilder forgotPassword(String email) {
        return post(FORGOT_URL)
                .contentType("application/json")
                .content("""
                        {"email": "%s"}
                        """.formatted(email));
    }

    private MockHttpServletRequestBuilder resetPassword(String token, String password) {
        return post(RESET_URL)
                .contentType("application/json")
                .content("""
                        {"token": "%s", "password": "%s"}
                        """.formatted(token, password));
    }

    private MockHttpServletRequestBuilder login(String email, String password) {
        return post(LOGIN_URL)
                .contentType("application/json")
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    @TestConfiguration
    static class TestEmailConfig {
        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    /** Captura los emails "enviados" en vez de mandarlos — ver el javadoc de la clase. */
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
