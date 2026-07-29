package com.frontpet.orders.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pedido creado por el checkout público (V4__orders.sql).
 *
 * <p>{@code publicId} lo asigna el service con {@link com.frontpet.common.UuidV7}
 * antes de persistir, igual que {@code catalog.domain.Product}.
 *
 * <p>Primer uso de {@code @Enumerated(EnumType.STRING)} del repo: hasta ahora
 * ningún módulo mapeaba un enum a columna. Calza con las longitudes de
 * {@code V4__orders.sql} ({@code status VARCHAR(12)}, {@code frete_mode
 * VARCHAR(12)}, {@code forma_pagamento VARCHAR(40)}).
 */
@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "cliente_nome", nullable = false, length = 160)
    private String clienteNome;

    @Column(name = "cliente_telefone", nullable = false, length = 30)
    private String clienteTelefone;

    @Column(name = "cliente_telefone_norm", nullable = false, length = 20)
    private String clienteTelefoneNorm;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 40)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "frete_mode", nullable = false, length = 12)
    private FreteMode freteMode;

    @Column(name = "endereco_entrega", nullable = false, columnDefinition = "TEXT")
    private String enderecoEntrega;

    @Column(name = "horario_entrega", length = 120)
    private String horarioEntrega;

    @Column(name = "subtotal_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalSnapshot;

    // Columnas propias, no aportadas por Auditable: created_at/updated_at
    // marcan la fila en sí, confirmed_at/cancelled_at marcan la transición
    // de status (ADR 003).
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    // Clave de negocio: publicId, asignado por el service ANTES del INSERT
    // (igual razón que Product.equals).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return publicId != null && publicId.equals(other.getPublicId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
