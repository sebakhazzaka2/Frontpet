package com.frontpet.identity;

import com.frontpet.identity.domain.AdminUser;
import com.frontpet.identity.domain.AdminUserRepository;
import com.frontpet.identity.domain.PasswordResetToken;
import com.frontpet.identity.domain.PasswordResetTokenRepository;
import com.frontpet.notifications.EmailMessage;
import com.frontpet.notifications.EmailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test puro (sin Spring), como {@code LoginAttemptServiceTest}: Mockito
 * para los repos/encoder, un {@link FakeEmailSender} que captura el
 * {@link EmailMessage} en vez de un {@code EmailSender} real, y un
 * {@link MutableClock} propio para no depender de {@code Thread.sleep}.
 *
 * <p>{@code PlatformTransactionManager} va mockeado a propósito: el service
 * usa un {@code TransactionTemplate} imperativo (no {@code @Transactional})
 * y acá no hay contexto de Spring ni DB real detrás — solo hace falta que
 * {@code getTransaction()} devuelva algo no nulo para que el callback corra.
 */
class PasswordResetServiceTest {

    private static final Duration TTL = Duration.ofMinutes(60);
    private static final String APP_BASE_URL = "https://frontpet.com.br";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-29T12:00:00Z"));
    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final FakeEmailSender emailSender = new FakeEmailSender();
    private final PasswordResetProperties properties = new PasswordResetProperties(
            TTL,
            new PasswordResetProperties.RateLimit(true, 5, Duration.ofMinutes(15)),
            new PasswordResetProperties.RateLimit(true, 3, Duration.ofHours(1)));
    private final PasswordResetEmailAttemptService emailAttempts =
            new PasswordResetEmailAttemptService(clock, properties);

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(txManager.getTransaction(any())).thenReturn(status);

        service = new PasswordResetService(
                adminUserRepository, tokenRepository, emailAttempts, passwordEncoder,
                emailSender, properties, clock, txManager, APP_BASE_URL);

        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "{bcrypt}" + inv.getArgument(0));
    }

    @Test
    @DisplayName("el token generado tiene 43 chars Base64URL sin padding (32 bytes de SecureRandom)")
    void tokenIsUrlSafe43Chars() {
        stubExistingUser("admin@frontpet.dev");

        service.requestReset("admin@frontpet.dev", "203.0.113.10");

        String token = extractToken(emailSender.sent.get(0));
        assertThat(token).hasSize(43);
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("dos pedidos consecutivos generan tokens distintos")
    void consecutiveTokensAreDifferent() {
        stubExistingUser("admin@frontpet.dev");

        service.requestReset("admin@frontpet.dev", "203.0.113.10");
        service.requestReset("admin@frontpet.dev", "203.0.113.10");

        String first = extractToken(emailSender.sent.get(0));
        String second = extractToken(emailSender.sent.get(1));
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("el token en claro nunca se persiste — el hash guardado no coincide con el token del link")
    void rawTokenIsNeverPersisted() {
        stubExistingUser("admin@frontpet.dev");

        service.requestReset("admin@frontpet.dev", "203.0.113.10");

        String rawToken = extractToken(emailSender.sent.get(0));
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(captor.getValue().getTokenHash()).isEqualTo(sha256Hex(rawToken));
    }

    @Test
    @DisplayName("el link del email usa la base URL configurada")
    void linkUsesConfiguredBaseUrl() {
        stubExistingUser("admin@frontpet.dev");

        service.requestReset("admin@frontpet.dev", "203.0.113.10");

        assertThat(emailSender.sent.get(0).htmlBody())
                .contains(APP_BASE_URL + "/admin/redefinir-senha?token=");
    }

    @Test
    @DisplayName("email desconocido no lanza y no envía nada")
    void unknownEmailDoesNotThrowOrSend() {
        when(adminUserRepository.findByEmail("naoexiste@frontpet.dev")).thenReturn(Optional.empty());

        service.requestReset("naoexiste@frontpet.dev", "203.0.113.10");

        assertThat(emailSender.sent).isEmpty();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("un token de 61 minutos está expirado")
    void tokenExpiresAfter61Minutes() {
        RequestResult result = requestResetAndCapture("admin@frontpet.dev", "203.0.113.10");

        clock.advance(Duration.ofMinutes(61));

        assertThatThrownBy(() -> service.resetPassword(result.rawToken(), "nova-senha-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Link de redefinição inválido ou expirado.");
    }

    @Test
    @DisplayName("límite del TTL: a TTL - 1s el token todavía funciona")
    void ttlBoundaryJustBeforeExpirationWorks() {
        RequestResult result = requestResetAndCapture("admin@frontpet.dev", "203.0.113.10");

        clock.advance(TTL.minusSeconds(1));

        service.resetPassword(result.rawToken(), "nova-senha-123");
        // Não lança — o assert real é a ausência de exceção.
    }

    @Test
    @DisplayName("límite del TTL: a exactamente TTL el token ya no funciona")
    void ttlBoundaryAtExpirationFails() {
        RequestResult result = requestResetAndCapture("admin@frontpet.dev", "203.0.113.10");

        clock.advance(TTL);

        assertThatThrownBy(() -> service.resetPassword(result.rawToken(), "nova-senha-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Link de redefinição inválido ou expirado.");
    }

    @Test
    @DisplayName("un token ya usado se rechaza en el segundo intento")
    void usedTokenIsRejected() {
        RequestResult result = requestResetAndCapture("admin@frontpet.dev", "203.0.113.10");

        service.resetPassword(result.rawToken(), "nova-senha-123");

        assertThatThrownBy(() -> service.resetPassword(result.rawToken(), "outra-senha-456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Link de redefinição inválido ou expirado.");
    }

    @Test
    @DisplayName("token inexistente y token expirado devuelven el mismo mensaje literal")
    void nonExistentAndExpiredTokensGiveSameMessage() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("token-que-nao-existe", "nova-senha-123"))
                .hasMessage("Link de redefinição inválido ou expirado.");

        RequestResult result = requestResetAndCapture("outro@frontpet.dev", "203.0.113.11");
        clock.advance(TTL);
        assertThatThrownBy(() -> service.resetPassword(result.rawToken(), "nova-senha-123"))
                .hasMessage("Link de redefinição inválido ou expirado.");
    }

    @Test
    @DisplayName("el reset actualiza passwordHash y passwordChangedAt juntos")
    void resetUpdatesPasswordAndChangedAt() {
        RequestResult result = requestResetAndCapture("admin@frontpet.dev", "203.0.113.10");
        Instant originalChangedAt = result.persisted().getAdminUser().getPasswordChangedAt();

        clock.advance(Duration.ofMinutes(5));
        service.resetPassword(result.rawToken(), "nova-senha-123");

        AdminUser user = result.persisted().getAdminUser();
        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}nova-senha-123");
        assertThat(user.getPasswordChangedAt()).isAfter(originalChangedAt);
    }

    @Test
    @DisplayName("pedir un token nuevo borra los pendientes del mismo admin")
    void requestingNewTokenDeletesPrevious() {
        AdminUser user = stubExistingUser("admin@frontpet.dev");

        service.requestReset("admin@frontpet.dev", "203.0.113.10");
        service.requestReset("admin@frontpet.dev", "203.0.113.10");

        verify(tokenRepository, times(2)).deleteByAdminUserId(user.getId());
    }

    @Test
    @DisplayName("superar el límite por email no envía el 4to mail y tampoco lanza")
    void exceedingEmailRateLimitStopsSendingSilently() {
        stubExistingUser("admin@frontpet.dev");

        service.requestReset("admin@frontpet.dev", "203.0.113.10"); // 1
        service.requestReset("admin@frontpet.dev", "203.0.113.10"); // 2
        service.requestReset("admin@frontpet.dev", "203.0.113.10"); // 3 — alcanza max-attempts=3, bloquea
        service.requestReset("admin@frontpet.dev", "203.0.113.10"); // 4 — bloqueado, no debería enviar

        assertThat(emailSender.sent).hasSize(3);
    }

    // ---- helpers ------------------------------------------------------

    private AdminUser stubExistingUser(String email) {
        AdminUser user = adminUser(email);
        when(adminUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return user;
    }

    private AdminUser adminUser(String email) {
        AdminUser user = new AdminUser();
        user.setId(1L);
        user.setTenantId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole("ADMIN");
        user.changePassword("{bcrypt}senha-antiga", clock.instant());
        return user;
    }

    private record RequestResult(String rawToken, PasswordResetToken persisted) {
    }

    /** Pide un reset, captura el token persistido y encadena findByTokenHash para poder consumirlo después. */
    private RequestResult requestResetAndCapture(String email, String ip) {
        stubExistingUser(email);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        service.requestReset(email, ip);
        verify(tokenRepository, atLeastOnce()).save(captor.capture());
        PasswordResetToken persisted = captor.getValue();

        String rawToken = extractToken(emailSender.sent.get(emailSender.sent.size() - 1));
        when(tokenRepository.findByTokenHash(persisted.getTokenHash())).thenReturn(Optional.of(persisted));
        return new RequestResult(rawToken, persisted);
    }

    private static String extractToken(EmailMessage message) {
        Matcher matcher = TOKEN_PATTERN.matcher(message.htmlBody());
        if (!matcher.find()) {
            throw new IllegalStateException("Link de reset não encontrado no corpo do e-mail simulado.");
        }
        return matcher.group(1);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class FakeEmailSender implements EmailSender {
        private final List<EmailMessage> sent = new ArrayList<>();

        @Override
        public void send(EmailMessage message) {
            sent.add(message);
        }
    }

    /** {@link Clock} mutable para avanzar el tiempo sin {@code Thread.sleep} (mismo patrón que LoginAttemptServiceTest). */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
