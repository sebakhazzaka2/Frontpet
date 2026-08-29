package com.frontpet.privacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Auditoria de solicitações de eliminação LGPD atendidas (V13, ADR 023).
 * Append-only: sem {@code updated_at}, sem setters — um registro de
 * auditoria não se edita depois de escrito.
 *
 * <p>Guarda {@code telefoneHash} (SHA-256), não o telefone: reter o
 * identificador que acabou de ser removido do titular contradiria a própria
 * eliminação.
 *
 * <p>{@code adminUserId} não tem FK para {@code admin_users} — nenhuma
 * tabela do repo referencia essa tabela, porque os testes constroem
 * {@code AdminUser} em memória sem persistir (ver
 * {@code AdminOrderControllerTest.adminFor}). Uma FK aqui quebraria todo
 * teste deste endpoint.
 */
@Entity
@Table(name = "privacy_erasure_log")
@Getter
@NoArgsConstructor
public class PrivacyErasureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "admin_user_id", nullable = false, updatable = false)
    private Long adminUserId;

    @Column(name = "telefone_hash", nullable = false, updatable = false, length = 64)
    private String telefoneHash;

    @Column(name = "orders_anonymized", nullable = false, updatable = false)
    private int ordersAnonymized;

    @Column(name = "appointments_anonymized", nullable = false, updatable = false)
    private int appointmentsAnonymized;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PrivacyErasureLog(UUID tenantId, Long adminUserId, String telefoneHash,
                              int ordersAnonymized, int appointmentsAnonymized) {
        this.tenantId = tenantId;
        this.adminUserId = adminUserId;
        this.telefoneHash = telefoneHash;
        this.ordersAnonymized = ordersAnonymized;
        this.appointmentsAnonymized = appointmentsAnonymized;
    }
}
