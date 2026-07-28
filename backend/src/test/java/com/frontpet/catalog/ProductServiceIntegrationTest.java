package com.frontpet.catalog;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.domain.Category;
import com.frontpet.catalog.domain.CategoryRepository;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.catalog.domain.ProductVariant;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductSummary;
import com.frontpet.common.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Happy path del catálogo contra Postgres real.
 *
 * <p>El objetivo principal no es cubrir ramas: es <b>ejecutar</b> las queries.
 * Que el contexto de Spring levante solo prueba que el JPQL parsea; recién
 * cuando la query corre se sabe si Postgres pudo inferir los tipos de los
 * parámetros opcionales ({@code :categorySlug IS NULL}).
 *
 * <p>{@code @Transactional} en la clase hace rollback al terminar cada test,
 * así la DB local no queda sucia.
 */
@SpringBootTest
@Transactional
class ProductServiceIntegrationTest extends AbstractIntegrationTest {

    /** Tenant sembrado en V5__seed_dev.sql. */
    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private Category racoes;

    @BeforeEach
    void setUp() {
        racoes = categoryRepository.findByTenantIdAndSlug(TENANT, "racoes").orElseThrow();
    }

    @Test
    @DisplayName("lista productos activos sin filtros")
    void listsActiveProducts() {
        persistProduct("Ração Golden 15kg", "racao-golden-15kg", new BigDecimal("189.90"));

        Page<ProductSummary> page = productService.list(
                TENANT, null, null, false, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(ProductSummary::slug)
                .contains("racao-golden-15kg");
    }

    @Test
    @DisplayName("el producto inactivo no aparece en el catálogo")
    void hidesInactiveProducts() {
        Product hidden = persistProduct("Produto Oculto", "produto-oculto", new BigDecimal("10.00"));
        productService.setActive(TENANT, hidden.getPublicId(), false);

        Page<ProductSummary> page = productService.list(
                TENANT, null, null, false, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(ProductSummary::slug)
                .doesNotContain("produto-oculto");
    }

    @Test
    @DisplayName("filtra por categoría y descarta las que no coinciden")
    void filtersByCategory() {
        Product p = persistProduct("Ração Premium", "racao-premium", new BigDecimal("99.90"));
        p.getCategories().add(racoes);
        productRepository.saveAndFlush(p);

        assertThat(productService.list(TENANT, "racoes", null, false, PageRequest.of(0, 20)))
                .extracting(ProductSummary::slug)
                .contains("racao-premium");

        assertThat(productService.list(TENANT, "brinquedos", null, false, PageRequest.of(0, 20)))
                .extracting(ProductSummary::slug)
                .doesNotContain("racao-premium");
    }

    @Test
    @DisplayName("busca por nombre ignorando mayúsculas")
    void searchesByName() {
        persistProduct("Brinquedo Mordedor", "brinquedo-mordedor", new BigDecimal("29.90"));

        assertThat(productService.list(TENANT, null, "MORDEDOR", false, PageRequest.of(0, 20)))
                .extracting(ProductSummary::slug)
                .contains("brinquedo-mordedor");
    }

    @Test
    @DisplayName("onlyOnSale devuelve solo los que tienen precio tachado")
    void filtersOnSale() {
        Product normal = persistProduct("Sem Desconto", "sem-desconto", new BigDecimal("50.00"));
        Product promo = persistProduct("Com Desconto", "com-desconto", new BigDecimal("40.00"));
        promo.setPriceOriginal(new BigDecimal("60.00"));
        productRepository.saveAndFlush(promo);

        List<String> slugs = productService
                .list(TENANT, null, null, true, PageRequest.of(0, 20))
                .map(ProductSummary::slug)
                .getContent();

        assertThat(slugs).contains(promo.getSlug()).doesNotContain(normal.getSlug());
    }

    @Test
    @DisplayName("con variantes, el precio del listado es el de la más barata")
    void usesCheapestVariantPrice() {
        Product p = persistProduct("Ração com Variantes", "racao-variantes", null);
        p.getVariants().add(variant(p, "3kg", new BigDecimal("59.90")));
        p.getVariants().add(variant(p, "15kg", new BigDecimal("189.90")));
        productRepository.saveAndFlush(p);

        ProductSummary summary = productService
                .list(TENANT, null, "Ração com Variantes", false, PageRequest.of(0, 20))
                .getContent().getFirst();

        assertThat(summary.price()).isEqualByComparingTo("59.90");
        assertThat(summary.hasVariants()).isTrue();
    }

    @Test
    @DisplayName("el detalle trae variantes ordenadas por precio y sus categorías")
    void returnsDetailWithRelations() {
        Product p = persistProduct("Ração Detalhe", "racao-detalhe", null);
        p.getCategories().add(racoes);
        p.getVariants().add(variant(p, "15kg", new BigDecimal("189.90")));
        p.getVariants().add(variant(p, "3kg", new BigDecimal("59.90")));
        productRepository.saveAndFlush(p);

        ProductDetail detail = productService.getBySlug(TENANT, "racao-detalhe");

        assertThat(detail.variants()).extracting(v -> v.nomeVariante()).containsExactly("3kg", "15kg");
        assertThat(detail.categories()).extracting(c -> c.slug()).containsExactly("racoes");
        assertThat(detail.hasVariants()).isTrue();
    }

    @Test
    @DisplayName("pedir un slug inexistente da ProductNotFound")
    void failsOnMissingSlug() {
        assertThatThrownBy(() -> productService.getBySlug(TENANT, "no-existe"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("getByPublicId trae el mismo detalle que getBySlug")
    void getsByPublicId() {
        Product p = persistProduct("Ração por PublicId", "racao-public-id", new BigDecimal("77.00"));

        ProductDetail detail = productService.getByPublicId(TENANT, p.getPublicId());

        assertThat(detail.publicId()).isEqualTo(p.getPublicId());
        assertThat(detail.slug()).isEqualTo("racao-public-id");
    }

    @Test
    @DisplayName("getByPublicId de un id inexistente da ProductNotFound")
    void failsOnMissingPublicId() {
        assertThatThrownBy(() -> productService.getByPublicId(TENANT, UuidV7.generate()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("el detalle de un producto oculto también da 404, no solo el listado")
    void hiddenProductDetailIsNotFound() {
        Product p = persistProduct("Oculto no Detalhe", "oculto-no-detalhe", new BigDecimal("15.00"));
        productService.setActive(TENANT, p.getPublicId(), false);

        assertThatThrownBy(() -> productService.getBySlug(TENANT, "oculto-no-detalhe"))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> productService.getByPublicId(TENANT, p.getPublicId()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("la página siguiente trae los productos que no entraron en la primera")
    void paginatesBeyondFirstPage() {
        for (int i = 1; i <= 3; i++) {
            persistProduct("Produto Paginado " + i, "produto-paginado-" + i, new BigDecimal("10.00"));
        }

        Page<ProductSummary> firstPage = productService.list(
                TENANT, null, "Produto Paginado", false, PageRequest.of(0, 2));
        Page<ProductSummary> secondPage = productService.list(
                TENANT, null, "Produto Paginado", false, PageRequest.of(1, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();

        // Ningún slug se repite entre páginas: si el countQuery estuviera
        // desalineado de la query principal, esto es lo que lo detectaría.
        List<String> allSlugs = new java.util.ArrayList<>(firstPage.map(ProductSummary::slug).getContent());
        allSlugs.addAll(secondPage.map(ProductSummary::slug).getContent());
        assertThat(allSlugs).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("el slug repetido recibe sufijo numérico")
    void generatesUniqueSlug() {
        assertThat(productService.generateUniqueSlug(TENANT, "Ração Golden")).isEqualTo("racao-golden");

        persistProduct("Ração Golden", "racao-golden", new BigDecimal("100.00"));

        assertThat(productService.generateUniqueSlug(TENANT, "Ração Golden")).isEqualTo("racao-golden-2");
    }

    // ---- helpers -------------------------------------------------------

    private Product persistProduct(String nome, String slug, BigDecimal price) {
        Product p = new Product();
        p.setPublicId(UuidV7.generate());
        p.setTenantId(TENANT);
        p.setNome(nome);
        p.setSlug(slug);
        p.setPrice(price);
        p.setStock(10);
        p.setActive(true);
        return productRepository.saveAndFlush(p);
    }

    private ProductVariant variant(Product product, String nome, BigDecimal price) {
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setNomeVariante(nome);
        v.setPrice(price);
        v.setStock(5);
        v.setActive(true);
        return v;
    }
}
