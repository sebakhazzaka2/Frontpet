package com.frontpet.catalog.api;

import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.common.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints públicos del catálogo, sobre el stack HTTP real.
 *
 * <p>Los tests del service ya cubren las queries; acá lo que se prueba es la
 * capa de encima: que las rutas existan, que los parámetros de query se aten
 * bien, que el JSON tenga la forma que el frontend espera y que un 404 salga
 * como 404 y no como 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicCatalogControllerTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;

    @Test
    @DisplayName("GET /api/v1/products devuelve la página con la forma esperada")
    void listsProducts() throws Exception {
        persistProduct("Ração Golden 15kg", "racao-golden-15kg", new BigDecimal("189.90"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    @DisplayName("GET /api/v1/products?busca= filtra por nombre")
    void searchesProducts() throws Exception {
        persistProduct("Brinquedo Mordedor", "brinquedo-mordedor", new BigDecimal("29.90"));
        persistProduct("Ração Premium", "racao-premium-busca", new BigDecimal("99.90"));

        mockMvc.perform(get("/api/v1/products").param("busca", "mordedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("brinquedo-mordedor"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/products/{slug} devuelve el detalle")
    void getsProductDetail() throws Exception {
        persistProduct("Ração Detalhe API", "racao-detalhe-api", new BigDecimal("149.90"));

        mockMvc.perform(get("/api/v1/products/racao-detalhe-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("racao-detalhe-api"))
                .andExpect(jsonPath("$.nome").value("Ração Detalhe API"))
                .andExpect(jsonPath("$.price").value(149.90))
                .andExpect(jsonPath("$.variants").isArray())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    @DisplayName("un slug inexistente da 404 con cuerpo de error, no 500")
    void missingProductReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/no-existe-mesmo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado."))
                .andExpect(jsonPath("$.path").value("/api/v1/products/no-existe-mesmo"));
    }

    @Test
    @DisplayName("GET /api/v1/categories devuelve las 7 sembradas")
    void listsCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[?(@.slug == 'racoes')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'outros')]").exists());
    }

    @Test
    @DisplayName("GET /api/v1/species devuelve solo Cães y Gatos")
    void listsSpecies() throws Exception {
        mockMvc.perform(get("/api/v1/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.slug == 'caes')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'gatos')]").exists());
    }

    @Test
    @DisplayName("size por encima del tope se recorta a max-page-size")
    void capsPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

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
}
