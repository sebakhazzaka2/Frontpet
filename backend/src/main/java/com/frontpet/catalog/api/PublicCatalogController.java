package com.frontpet.catalog.api;

import com.frontpet.catalog.CategoryService;
import com.frontpet.catalog.ProductService;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductSummary;
import com.frontpet.catalog.dto.TaxonRef;
import com.frontpet.common.PageResponse;
import com.frontpet.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints públicos del catálogo — sin auth, los consume la web pública.
 *
 * <p>El tenant no viaja en la URL ni en un header: lo resuelve
 * {@link CurrentTenant}. Un endpoint público donde el cliente elige el tenant
 * sería un agujero en cuanto el sistema sea multi-tenant.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CurrentTenant currentTenant;

    /**
     * Catálogo paginado. Los tres filtros son opcionales y se combinan:
     * {@code /api/v1/products?categoria=racoes&busca=golden&page=0&size=20}
     */
    @GetMapping("/products")
    public PageResponse<ProductSummary> listProducts(
            @RequestParam(name = "categoria", required = false) String categorySlug,
            @RequestParam(name = "busca", required = false) String search,
            @RequestParam(name = "promocoes", defaultValue = "false") boolean onlyOnSale,
            @PageableDefault(size = 24) Pageable pageable) {

        return PageResponse.of(productService.list(
                currentTenant.id(), categorySlug, search, onlyOnSale, pageable));
    }

    /** Detalle del producto. 404 si no existe o está oculto. */
    @GetMapping("/products/{slug}")
    public ProductDetail getProduct(@PathVariable String slug) {
        return productService.getBySlug(currentTenant.id(), slug);
    }

    /** Categorías para los chips del filtro. */
    @GetMapping("/categories")
    public List<TaxonRef> listCategories() {
        return categoryService.listCategories(currentTenant.id());
    }

    /** Espécies (Cães, Gatos). */
    @GetMapping("/species")
    public List<TaxonRef> listSpecies() {
        return categoryService.listSpecies(currentTenant.id());
    }
}
