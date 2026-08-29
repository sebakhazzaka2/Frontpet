package com.frontpet.identity;

import com.frontpet.identity.domain.AdminUser;
import com.frontpet.identity.domain.AdminUserRepository;
import com.frontpet.identity.domain.PasswordResetToken;
import com.frontpet.identity.domain.PasswordResetTokenRepository;
import com.frontpet.notifications.EmailMessage;
import com.frontpet.notifications.EmailSender;
import com.frontpet.notifications.PasswordResetEmailTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Flujo de reset de contraseña del admin (tarea 7.12, ADR 022): pedir el
 * reset por email y, con el token recibido, setear una contraseña nueva.
 *
 * <p>Token opaco de 32 bytes de {@link SecureRandom}, Base64URL sin padding
 * (43 chars). Se persiste solo su sha256 en hex, nunca el valor en claro —
 * ver el header de {@code V12__admin_password_reset.sql} para el
 * razonamiento completo (por qué no BCrypt, por qué no JWT).
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;

    private final AdminUserRepository adminUserRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetEmailAttemptService emailAttempts;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final String appBaseUrl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            AdminUserRepository adminUserRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetEmailAttemptService emailAttempts,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            PasswordResetProperties properties,
            Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${frontpet.app.base-url}") String appBaseUrl) {
        this.adminUserRepository = adminUserRepository;
        this.tokenRepository = tokenRepository;
        this.emailAttempts = emailAttempts;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.properties = properties;
        this.clock = clock;
        // TransactionTemplate imperativo, no @Transactional: necesito que el
        // INSERT del token esté COMMITEADO antes de disparar el email (ver
        // requestReset). Con @Transactional en un método de esta misma
        // clase, llamarlo desde acá adentro no pasaría por el proxy de
        // Spring y no sería transaccional de verdad (self-invocation).
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    /**
     * Nunca lanza por email desconocido, ni distingue en la respuesta si el
     * email existe, está bloqueado por rate limit, o el mail no se pudo
     * mandar — {@code AuthController} siempre devuelve 202. Ver ADR 022 para
     * el detalle completo de anti-enumeração.
     */
    public void requestReset(String rawEmail, String ip) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String emailKey = "email:" + email;

        if (emailAttempts.blockedSecondsRemaining(emailKey) > 0) {
            // Sin 429 acá: un 429 por email sería un oráculo de enumeração
            // distinto al que ya evita el 202 constante.
            log.warn("Pedido de redefinição de senha bloqueado por rate limit de e-mail: {}", maskEmail(email));
            return;
        }
        // Se cuenta SIEMPRE, exista o no el usuario — contar solo los
        // existentes sería otro oráculo, esta vez a los N intentos.
        emailAttempts.recordAttempt(emailKey);

        Optional<AdminUser> maybeUser = adminUserRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            log.info("Pedido de redefinição de senha para e-mail não cadastrado.");
            return;
        }
        AdminUser user = maybeUser.get();

        String rawToken = generateToken();
        String tokenHash = sha256Hex(rawToken);
        Instant expiresAt = clock.instant().plus(properties.ttl());

        // Pedir un token nuevo invalida los pendientes y de paso limpia los
        // usados/expirados del mismo admin, sin necesitar un @Scheduled.
        transactionTemplate.executeWithoutResult(status -> {
            tokenRepository.deleteByAdminUserId(user.getId());
            tokenRepository.save(new PasswordResetToken(user, tokenHash, expiresAt, ip));
        });

        // Recién después del commit: si el INSERT hubiera fallado en el
        // flush, no queremos haber mandado un link que ya no sirve.
        String link = appBaseUrl + "/admin/redefinir-senha?token=" + rawToken;
        emailSender.send(new EmailMessage(
                user.getEmail(),
                PasswordResetEmailTemplate.subject(),
                PasswordResetEmailTemplate.text(link),
                PasswordResetEmailTemplate.html(link)));
    }

    /**
     * @throws IllegalArgumentException token inexistente, expirado o ya
     * usado — el mismo mensaje en los tres casos, a propósito: no distinguir
     * el motivo hacia afuera (ver ADR 022).
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(PasswordResetService::invalidTokenException);

        Instant now = clock.instant();
        if (!token.isUsable(now)) {
            throw invalidTokenException();
        }

        AdminUser user = token.getAdminUser();
        user.changePassword(passwordEncoder.encode(newPassword), now);
        token.markUsed(now);

        // El éxito libera el presupuesto de rate limit por email: quien
        // acaba de demostrar control del email no debería seguir pagando
        // por los intentos previos.
        emailAttempts.reset("email:" + user.getEmail());
    }

    private static IllegalArgumentException invalidTokenException() {
        return new IllegalArgumentException("Link de redefinição inválido ou expirado.");
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es un algoritmo garantizado por la especificación de
            // la JCA en toda JVM — esto no debería poder pasar nunca.
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
