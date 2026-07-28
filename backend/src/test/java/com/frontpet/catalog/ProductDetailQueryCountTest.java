package com.frontpet.catalog;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.catalog.domain.ProductVariant;
import com.frontpet.common.UuidV7;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba que el {@code @EntityGraph} de {@code ProductRepository.findByTenantIdAndSlug}
 * cumple lo que promete: traer producto + marca + variantes + categorías +
 * especies en pocos viajes a la DB, sin importar cuántas variantes tenga.
 *
 * <p>Sin el {@code @EntityGraph}, cada colección (variants, categories, species)
 * dispara su propia query lazy la primera vez que {@code toDetail()} la toca —
 * funciona porque la transacción sigue abierta, pero son 3-4 round-trips en
 * vez de uno. Este test no falla por una {@code LazyInitializationException}
 * (nunca ocurriría acá: el service es {@code @Transactional}); falla si el
 * conteo de queries deja de estar acotado.
 *
 * <p>Clase de test separada, con {@code generate_statistics} habilitado solo
 * acá: es una propiedad cara de tener prendida en todo el resto de la suite.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class ProductDetailQueryCountTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("el detalle no dispara una query extra por variante (nada de N+1)")
    void detailQueryCountDoesNotGrowWithVariantCount() {
        Product p = new Product();
        p.setPublicId(UuidV7.generate());
        p.setTenantId(TENANT);
        p.setNome("Ração com Muitas Variantes");
        p.setSlug("racao-muitas-variantes");
        p.setStock(0);
        p.setActive(true);
        for (int i = 1; i <= 8; i++) {
            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setNomeVariante(i + "kg");
            v.setPrice(new BigDecimal("10.00").multiply(BigDecimal.valueOf(i)));
            v.setStock(5);
            v.setActive(true);
            p.getVariants().add(v);
        }
        productRepository.saveAndFlush(p);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        productService.getBySlug(TENANT, "racao-muitas-variantes");

        long queryCount = stats.getPrepareStatementCount();
        // El número exacto depende de cómo Hibernate combina el bag (variants)
        // con los sets (categories, species) en el EntityGraph — lo que
        // importa es que sea una constante chica, no una por variante. 8
        // variantes con N+1 real serían 8+ queries adicionales; esto se queda
        // en un puñado fijo.
        assertThat(queryCount).isLessThanOrEqualTo(5);
    }
}
