package com.frontpet.identity.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Token opaco de un solo uso para el reset de contraseña del admin (tarea
 * 7.12, ADR 022). Solo guarda el hash SHA-256 del token, nunca el valor en
 * claro — ver el comentario de {@code V12__admin_password_reset.sql} para el
 * porqué (no BCrypt, no JWT).
 */
@Entity
@Table(name = "admin_password_reset_tokens")
@Getter @NoArgsConstructor
public class PasswordResetToken extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false, updatable = false)
    private AdminUser adminUser;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "requested_ip", length = 45, updatable = false)
    private String requestedIp;

    public PasswordResetToken(AdminUser adminUser, String tokenHash, Instant expiresAt, String requestedIp) {
        this.adminUser = adminUser;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestedIp = requestedIp;
    }

    /** @return true si el token todavía no fue usado y no expiró a {@code now}. */
    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant at) {
        this.usedAt = at;
    }
}
