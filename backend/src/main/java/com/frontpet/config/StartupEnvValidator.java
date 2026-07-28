package com.frontpet.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Portado de {@code config/StartupEnvValidator.java} del consultorio
 * (docs/reuse-consultorio.md §4, 🟢 "portar sí o sí"). Corre solo en perfil
 * {@code prod}: si falta una env var crítica o quedó el default inseguro de
 * dev, corta el arranque en vez de dejar el sistema mal configurado en vivo.
 */
@Component
@Profile("prod")
public class StartupEnvValidator implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupEnvValidator.class);

    private static final int JWT_SECRET_MIN_LENGTH = 32;
    private static final String INSECURE_ADMIN_PASSWORD = "admin123";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${frontpet.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${frontpet.admin.email}")
    private String adminEmail;

    @Value("${frontpet.admin.password}")
    private String adminPassword;

    @Value("${frontpet.r2.account-id}")
    private String r2AccountId;

    @Value("${frontpet.r2.access-key-id}")
    private String r2AccessKeyId;

    @Value("${frontpet.r2.secret-access-key}")
    private String r2SecretAccessKey;

    @Value("${frontpet.r2.bucket-name}")
    private String r2BucketName;

    @Value("${frontpet.r2.endpoint}")
    private String r2Endpoint;

    @Value("${frontpet.r2.public-url}")
    private String r2PublicUrl;

    private boolean validated = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (validated) {
            return;
        }
        validated = true;

        List<String> errors = new ArrayList<>();

        if (isBlank(jwtSecret)) {
            errors.add("JWT_SECRET is not set");
        } else if (jwtSecret.length() < JWT_SECRET_MIN_LENGTH) {
            errors.add("JWT_SECRET must be at least " + JWT_SECRET_MIN_LENGTH + " characters (got " + jwtSecret.length() + ")");
        }

        if (isBlank(corsAllowedOrigins)) {
            errors.add("CORS_ALLOWED_ORIGINS is not set");
        }

        if (isBlank(adminEmail)) {
            errors.add("ADMIN_EMAIL is not set");
        }

        if (isBlank(adminPassword)) {
            errors.add("ADMIN_PASSWORD is not set");
        } else if (INSECURE_ADMIN_PASSWORD.equals(adminPassword)) {
            errors.add("ADMIN_PASSWORD must not be the default development value");
        }

        if (isBlank(r2AccountId)) {
            errors.add("R2_ACCOUNT_ID is not set");
        }
        if (isBlank(r2AccessKeyId)) {
            errors.add("R2_ACCESS_KEY_ID is not set");
        }
        if (isBlank(r2SecretAccessKey)) {
            errors.add("R2_SECRET_ACCESS_KEY is not set");
        }
        if (isBlank(r2BucketName)) {
            errors.add("R2_BUCKET_NAME is not set");
        }
        if (isBlank(r2Endpoint)) {
            errors.add("R2_ENDPOINT is not set");
        }
        if (isBlank(r2PublicUrl)) {
            errors.add("R2_PUBLIC_URL is not set");
        }

        if (!errors.isEmpty()) {
            log.error("=== STARTUP FAILED: missing or insecure environment variables ===");
            errors.forEach(e -> log.error("  - {}", e));
            log.error("Set the required variables and restart the application.");
            System.exit(1);
        }

        log.info("Environment validation passed.");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
