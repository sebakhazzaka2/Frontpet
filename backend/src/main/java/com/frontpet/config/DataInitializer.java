package com.frontpet.config;

import com.frontpet.identity.domain.AdminUser;
import com.frontpet.identity.domain.AdminUserRepository;
import com.frontpet.tenant.CurrentTenant;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Portado de {@code config/DataInitializer.java} del consultorio
 * (docs/reuse-consultorio.md §4, 🟡): mismo patrón (seed idempotente desde env),
 * adaptado para colgar el admin del tenant único de MVP1 ({@link CurrentTenant}).
 * El tenant en sí no lo crea esto — lo sembra {@code V5__seed_dev.sql} en dev, y
 * el script de provisioning del Sprint Despliegue en producción.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentTenant currentTenant;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            CurrentTenant currentTenant,
            @Value("${frontpet.admin.email}") String adminEmail,
            @Value("${frontpet.admin.password}") String adminPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentTenant = currentTenant;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists, skipping seed: {}", adminEmail);
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setTenantId(currentTenant.id());
        admin.setEmail(adminEmail);
        admin.changePassword(passwordEncoder.encode(adminPassword), Instant.now());
        admin.setRole("ADMIN");
        adminUserRepository.save(admin);
        log.info("Admin user created from env: {}", adminEmail);
    }
}
