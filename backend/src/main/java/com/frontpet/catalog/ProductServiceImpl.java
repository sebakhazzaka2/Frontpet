package com.frontpet.catalog;

import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.catalog.domain.ProductVariant;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductSummary;
import com.frontpet.catalog.dto.ProductVariantDto;
import com.frontpet.catalog.dto.TaxonRef;
import com.frontpet.common.Slugify;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** Tope de sufijos al resolver colisiones de slug antes de rendirse. */
    private static final int MAX_SLUG_ATTEMPTS = 100;

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummary> list(UUID tenantId,
                                     String categorySlug,
                                     String search,
                                     boolean onlyOnSale,
                                     Pageable pageable) {
        // Un search en blanco es lo mismo que no buscar. Normalizarlo acá evita
        // que un LIKE '%%' inútil llegue a la DB desde un input vacío.
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        String normalizedCategory = (categorySlug == null || categorySlug.isBlank()) ? null : categorySlug;

        return productRepository.findSummaries(
                tenantId, normalizedCategory, normalizedSearch, onlyOnSale, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetail getBySlug(UUID tenantId, String slug) {
        Product product = productRepository.findByTenantIdAndSlug(tenantId, slug)
                .filter(Product::getActive)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produto não encontrado: " + slug));
        return toDetail(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetail getByPublicId(UUID tenantId, UUID publicId) {
        Product product = productRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .filter(Product::getActive)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produto não encontrado: " + publicId));
        return toDetail(product);
    }

    @Override
    @Transactional
    public void setActive(UUID tenantId, UUID publicId, boolean active) {
        Product product = productRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produto não encontrado: " + publicId));
        // Sin filtro por active: para volver a mostrar un producto oculto hay
        // que poder encontrarlo justamente estando oculto.
        product.setActive(active);
        // Sin save() explícito: la entidad está managed dentro de la transacción,
        // Hibernate detecta el cambio y hace el UPDATE al cerrar.
    }

    @Override
    @Transactional(readOnly = true)
    public String generateUniqueSlug(UUID tenantId, String nome) {
        String base = Slugify.slugify(nome);
        if (base.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não é possível gerar slug a partir de: " + nome);
        }

        if (!productRepository.existsByTenantIdAndSlug(tenantId, base)) {
            return base;
        }
        // Ocupado: probamos base-2, base-3... El UNIQUE(tenant_id, slug) de la DB
        // es la red de seguridad real si dos altas simultáneas eligen el mismo.
        for (int suffix = 2; suffix < MAX_SLUG_ATTEMPTS; suffix++) {
            String candidate = base + "-" + suffix;
            if (!productRepository.existsByTenantIdAndSlug(tenantId, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Não foi possível gerar um slug livre para: " + nome);
    }

    // ---- mapeo entidad → DTO -------------------------------------------
    // Se llama siempre dentro de una transacción abierta, con las colecciones
    // ya traídas por el @EntityGraph del repositorio. Para cuando el controller
    // serializa el resultado ya no queda nada lazy que pueda fallar.

    private ProductDetail toDetail(Product product) {
        List<ProductVariantDto> variants = product.getVariants().stream()
                .filter(ProductVariant::getActive)
                .sorted(Comparator.comparing(ProductVariant::getPrice))
                .map(v -> new ProductVariantDto(
                        v.getId(),
                        v.getNomeVariante(),
                        v.getPrice(),
                        v.getPriceOriginal(),
                        v.getStock()))
                .toList();

        List<TaxonRef> categories = product.getCategories().stream()
                .map(c -> new TaxonRef(c.getNome(), c.getSlug()))
                .sorted(Comparator.comparing(TaxonRef::nome))
                .toList();

        List<TaxonRef> species = product.getSpecies().stream()
                .map(s -> new TaxonRef(s.getNome(), s.getSlug()))
                .sorted(Comparator.comparing(TaxonRef::nome))
                .toList();

        return new ProductDetail(
                product.getPublicId(),
                product.getSlug(),
                product.getNome(),
                product.getDescricao(),
                product.getMainImageUrl(),
                product.getPrice(),
                product.getPriceOriginal(),
                product.getStock(),
                product.getBrand() == null ? null : product.getBrand().getNome(),
                categories,
                species,
                variants);
    }
}
