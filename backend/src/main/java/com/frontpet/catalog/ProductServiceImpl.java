package com.frontpet.catalog;

import com.frontpet.catalog.domain.Brand;
import com.frontpet.catalog.domain.Category;
import com.frontpet.catalog.domain.CategoryRepository;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.catalog.domain.ProductVariant;
import com.frontpet.catalog.domain.Species;
import com.frontpet.catalog.domain.SpeciesRepository;
import com.frontpet.catalog.dto.CreateProductRequest;
import com.frontpet.catalog.dto.OrderLineSnapshot;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductSummary;
import com.frontpet.catalog.dto.ProductVariantDto;
import com.frontpet.catalog.dto.ProductVariantRequest;
import com.frontpet.catalog.dto.ProductVariantUpsertRequest;
import com.frontpet.catalog.dto.TaxonRef;
import com.frontpet.catalog.dto.UpdateProductRequest;
import com.frontpet.common.Slugify;
import com.frontpet.common.UuidV7;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** Tope de sufijos al resolver colisiones de slug antes de rendirse. */
    private static final int MAX_SLUG_ATTEMPTS = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SpeciesRepository speciesRepository;
    private final BrandService brandService;

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

    @Override
    @Transactional
    public ProductDetail create(UUID tenantId, CreateProductRequest request) {
        boolean hasVariants = request.variants() != null && !request.variants().isEmpty();
        validarPricingInvariant(hasVariants, request.price(), request.priceOriginal());

        Product product = new Product();
        product.setPublicId(UuidV7.generate());
        product.setTenantId(tenantId);
        product.setNome(request.nome());
        product.setDescricao(request.descricao());
        product.setMainImageUrl(request.mainImageUrl());
        product.setSlug(generateUniqueSlug(tenantId, request.nome()));
        product.setActive(true);

        // price/stock del producto solo valen cuando NO hay variantes — ver el
        // comentario de Product.stock sobre esta asimetría con price.
        product.setPrice(hasVariants ? null : request.price());
        product.setPriceOriginal(hasVariants ? null : request.priceOriginal());
        product.setStock(hasVariants ? 0 : (request.stock() != null ? request.stock() : 0));

        product.setBrand(resolveBrand(tenantId, request.brandNome()));
        product.getCategories().addAll(resolveCategories(tenantId, request.categorySlugs()));
        product.getSpecies().addAll(resolveSpecies(tenantId, request.speciesSlugs()));

        if (hasVariants) {
            for (ProductVariantRequest variantRequest : request.variants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setNomeVariante(variantRequest.nomeVariante());
                variant.setPrice(variantRequest.price());
                variant.setStock(variantRequest.stock() != null ? variantRequest.stock() : 0);
                variant.setActive(true);
                product.getVariants().add(variant);
            }
        }

        Product saved = productRepository.save(product);
        return toDetail(saved);
    }

    @Override
    @Transactional
    public ProductDetail update(UUID tenantId, UUID publicId, UpdateProductRequest request) {
        Product product = productRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produto não encontrado: " + publicId));

        validarPricingInvariant(product.hasVariants(), request.price(), request.priceOriginal());

        product.setNome(request.nome());
        product.setDescricao(request.descricao());
        product.setMainImageUrl(request.mainImageUrl());

        if (!product.hasVariants()) {
            product.setPrice(request.price());
            // null limpa a promoção — reemplazo completo, igual que el resto
            // del record (docs/pending-decisions.md §3: antes no existía
            // ninguna forma de sacar un produto de oferta vía API).
            product.setPriceOriginal(request.priceOriginal());
            product.setStock(request.stock() != null ? request.stock() : 0);
        }

        product.setBrand(resolveBrand(tenantId, request.brandNome()));

        // Reemplazo completo de categorías/espécies: a diferencia de variants,
        // estas son tablas puente sin FK entrante de order_items — borrar y
        // reinsertar filas de product_categories/product_species no arriesga
        // ningún pedido histórico.
        product.getCategories().clear();
        product.getCategories().addAll(resolveCategories(tenantId, request.categorySlugs()));
        product.getSpecies().clear();
        product.getSpecies().addAll(resolveSpecies(tenantId, request.speciesSlugs()));

        if (request.slug() != null && !request.slug().isBlank()) {
            product.setSlug(resolveSlugForUpdate(tenantId, product, request.slug()));
        }

        // Sin save() explícito: managed dentro de la transacción.
        return toDetail(product);
    }

    @Override
    @Transactional
    public ProductDetail replaceVariants(UUID tenantId, UUID publicId,
                                         List<ProductVariantUpsertRequest> variants) {
        Product product = productRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produto não encontrado: " + publicId));

        if (!product.hasVariants()) {
            throw new IllegalArgumentException(
                    "Produto não tem variantes. Edite preço/estoque direto em PUT /admin/products/{id}.");
        }

        Map<Long, ProductVariant> existingById = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        Set<Long> keptIds = new HashSet<>();

        for (ProductVariantUpsertRequest request : variants) {
            if (request.id() == null) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setNomeVariante(request.nomeVariante());
                variant.setPrice(request.price());
                variant.setStock(request.stock() != null ? request.stock() : 0);
                variant.setActive(true);
                product.getVariants().add(variant);
            } else {
                ProductVariant existing = existingById.get(request.id());
                if (existing == null) {
                    throw new IllegalArgumentException(
                            "Variante " + request.id() + " não pertence a este produto.");
                }
                existing.setNomeVariante(request.nomeVariante());
                existing.setPrice(request.price());
                existing.setStock(request.stock() != null ? request.stock() : 0);
                existing.setActive(true);
                keptIds.add(request.id());
            }
        }

        // Soft-delete: la que no vino en el request se desactiva — nunca
        // list.remove(), eso dispara orphanRemoval y el DELETE choca contra
        // la FK de order_items en cuanto haya un pedido real (ver interfaz).
        // El filtro por getId() != null excluye las recién agregadas arriba
        // (todavía sin id, IDENTITY lo asigna al hacer flush).
        for (ProductVariant existing : product.getVariants()) {
            if (existing.getId() != null && !keptIds.contains(existing.getId())) {
                existing.setActive(false);
            }
        }

        boolean anyActive = product.getVariants().stream().anyMatch(ProductVariant::getActive);
        if (!anyActive) {
            throw new IllegalArgumentException("Produto precisa de ao menos uma variante ativa.");
        }

        // Sin save() explícito: managed dentro de la transacción.
        return toDetail(product);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderLineSnapshot resolveOrderLine(UUID tenantId, UUID productPublicId, Long variantId) {
        Product product = productRepository.findByTenantIdAndPublicId(tenantId, productPublicId)
                .filter(Product::getActive)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produto não encontrado: " + productPublicId));

        if (product.hasVariants()) {
            if (variantId == null) {
                throw new IllegalArgumentException(
                        "Produto \"" + product.getNome() + "\" exige a escolha de uma variante.");
            }
            ProductVariant variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(variantId) && v.getActive())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Variante " + variantId + " não pertence ao produto \"" + product.getNome() + "\"."));
            String nome = product.getNome() + " — " + variant.getNomeVariante();
            return new OrderLineSnapshot(
                    product.getId(),
                    variant.getId(),
                    nome.length() > 200 ? nome.substring(0, 200) : nome,
                    variant.getPrice());
        }

        if (variantId != null) {
            throw new IllegalArgumentException(
                    "Produto \"" + product.getNome() + "\" não tem variantes.");
        }
        return new OrderLineSnapshot(product.getId(), null, product.getNome(), product.getPrice());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getPublicIdById(UUID tenantId, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado: " + productId));
        return product.getPublicId();
    }

    // ---- helpers de negocio --------------------------------------------

    /**
     * ADR 013 §2: sin variantes, el precio vive en el producto y es
     * obligatorio; con variantes, el precio del producto no aplica —
     * exigirlo o aceptarlo en simultáneo son dos formas de dejar el dato en
     * un estado contradictorio que nadie va a notar hasta mostrarlo mal.
     *
     * <p>{@code priceOriginal} sigue la misma lógica: solo tiene sentido sin
     * variantes, y tiene que ser mayor que {@code price} — es el mismo
     * {@code CHECK(price_original > price)} de V6, validado acá para
     * devolver 400 con mensaje claro en vez de que Postgres lo rechace con
     * un 500 genérico.
     */
    private void validarPricingInvariant(boolean hasVariants, BigDecimal requestPrice,
                                         BigDecimal requestPriceOriginal) {
        if (!hasVariants && requestPrice == null) {
            throw new IllegalArgumentException(
                    "Produto sem variantes precisa de um preço.");
        }
        if (hasVariants && requestPrice != null) {
            throw new IllegalArgumentException(
                    "Produto com variantes não deve informar preço no nível do produto.");
        }
        if (hasVariants && requestPriceOriginal != null) {
            throw new IllegalArgumentException(
                    "Produto com variantes não deve informar preço original no nível do produto.");
        }
        if (requestPriceOriginal != null && requestPriceOriginal.compareTo(requestPrice) <= 0) {
            throw new IllegalArgumentException(
                    "Preço original deve ser maior que o preço de venda.");
        }
    }

    private Brand resolveBrand(UUID tenantId, String brandNome) {
        if (brandNome == null || brandNome.isBlank()) {
            return null;
        }
        return brandService.findOrCreate(tenantId, brandNome);
    }

    private Set<Category> resolveCategories(UUID tenantId, List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return Set.of();
        }
        return slugs.stream()
                .map(slug -> categoryRepository.findByTenantIdAndSlug(tenantId, slug)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Categoria não encontrada: " + slug)))
                .collect(Collectors.toSet());
    }

    private Set<Species> resolveSpecies(UUID tenantId, List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return Set.of();
        }
        return slugs.stream()
                .map(slug -> speciesRepository.findByTenantIdAndSlug(tenantId, slug)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Espécie não encontrada: " + slug)))
                .collect(Collectors.toSet());
    }

    /**
     * A diferencia del alta, acá el slug lo escribe el admin a mano — se
     * normaliza igual (evita espacios/mayúsculas rotos) pero, si colisiona,
     * se rechaza con 400 en vez de auto-sufijar en silencio. Auto-sufijar
     * tiene sentido cuando el sistema lo genera solo; si el admin edita un
     * valor específico y el sistema se lo cambia sin avisar, es confuso.
     */
    private String resolveSlugForUpdate(UUID tenantId, Product product, String requestedSlug) {
        String normalized = Slugify.slugify(requestedSlug);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não é possível gerar slug a partir de: " + requestedSlug);
        }
        if (normalized.equals(product.getSlug())) {
            return normalized;
        }
        if (productRepository.existsByTenantIdAndSlug(tenantId, normalized)) {
            throw new IllegalArgumentException(
                    "Já existe um produto com o slug: " + normalized);
        }
        return normalized;
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
