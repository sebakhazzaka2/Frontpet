package com.frontpet.identity.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "admin_users")
@Getter @Setter @NoArgsConstructor
public class AdminUser extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String role = "ADMIN";

    // Sin setter público a propósito (tarea 7.12): siempre cambia junto con
    // passwordHash vía changePassword(), nunca sola — ver JwtAuthFilter,
    // que compara esto contra el `iat` del JWT para invalidar sesiones.
    @Setter(AccessLevel.NONE)
    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    /**
     * Único mutador de la contraseña + su timestamp de cambio, atados en la
     * misma llamada para que {@code passwordChangedAt} no pueda quedar
     * desincronizado de {@code passwordHash} (ADR 022). La seed de
     * {@code DataInitializer} también pasa por acá, no solo el reset.
     */
    public void changePassword(String newPasswordHash, Instant at) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = at;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
