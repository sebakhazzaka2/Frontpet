package com.frontpet.orders.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Línea de un pedido. Hijo sin {@code publicId} propio, patrón
 * {@code catalog.domain.ProductVariant}.
 *
 * <p>{@code productId}/{@code productVariantId} son columnas planas
 * ({@code Long}), no {@code @ManyToOne} a {@code catalog.domain.Product} —
 * a propósito: {@code orders} no importa entidades de {@code catalog}
 * (CLAUDE.md §5, los módulos se hablan por la interfaz del service). La FK
 * simple de {@code product_id} y la compuesta
 * {@code (product_id, product_variant_id) → product_variants(product_id, id)}
 * las sigue garantizando Postgres, no Hibernate.
 */
@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "nome_snapshot", nullable = false, length = 200)
    private String nomeSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    private Integer quantidade;
}
