package com.frontpet.catalog.domain;

import com.frontpet.common.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor
public class ProductVariant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "nome_variante", nullable = false, length = 80)
    private String nomeVariante;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // NULL = sin promoción. Misma lógica que Product.priceOriginal.
    @Column(name = "price_original", precision = 10, scale = 2)
    private BigDecimal priceOriginal;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private Boolean active = true;

    public boolean isOnSale() {
        return priceOriginal != null;
    }
}
