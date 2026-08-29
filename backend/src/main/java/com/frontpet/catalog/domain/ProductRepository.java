package com.frontpet.catalog.domain;

import com.frontpet.catalog.dto.AdminProductSummary;
import com.frontpet.catalog.dto.ProductSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Listado del catálogo público, paginado.
     *
     * <p>Devuelve {@link ProductSummary} directo desde SQL en vez de entidades.
     * Dos motivos: no quedan colecciones lazy que puedan explotar cuando el
     * controller serializa (tenemos {@code open-in-view: false}), y la
     * paginación la hace Postgres de verdad. Si acá cargáramos entidades con
     * sus variantes, Hibernate no podría paginar en SQL y se traería la tabla
     * entera a memoria para recortarla ahí.
     *
     * <p>Los tres filtros son opcionales y van en una sola query a propósito:
     * el bloque {@code new ProductSummary(...)} es largo, y tenerlo repetido en
     * tres queries significa que agregar un campo obliga a tocar las tres.
     * Cuando un parámetro llega null, su condición se anula sola.
     *
     * <p>⚠️ El {@code CAST(:param AS String)} no es decorativo. Sin él, un
     * parámetro null viaja sin tipo y Postgres lo asume {@code bytea}, con lo
     * que {@code lower(bytea) does not exist} revienta la query. El síntoma es
     * traicionero: una vez que el statement se preparó con un valor real, el
     * caché lo tapa y los nulls posteriores funcionan.
     *
     * @param categorySlug filtra por categoría; null = todas
     * @param search       busca dentro del nombre; null = sin búsqueda
     */
    @Query(value = """
            SELECT new com.frontpet.catalog.dto.ProductSummary(
                p.publicId,
                p.slug,
                p.nome,
                p.mainImageUrl,
                COALESCE(p.price, (
                    SELECT MIN(v.price) FROM ProductVariant v
                    WHERE v.product = p AND v.active = true
                )),
                p.priceOriginal,
                b.nome,
                CASE WHEN EXISTS (
                    SELECT 1 FROM ProductVariant v2
                    WHERE v2.product = p AND v2.active = true
                ) THEN TRUE ELSE FALSE END
            )
            FROM Product p
            LEFT JOIN p.brand b
            WHERE p.tenantId = :tenantId
              AND p.active = true
              AND (CAST(:categorySlug AS String) IS NULL OR EXISTS (
                    SELECT 1 FROM Product px JOIN px.categories c
                    WHERE px = p AND c.slug = :categorySlug
              ))
              AND (CAST(:search AS String) IS NULL
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
              AND (:onlyOnSale = false OR p.priceOriginal IS NOT NULL)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.active = true
              AND (CAST(:categorySlug AS String) IS NULL OR EXISTS (
                    SELECT 1 FROM Product px JOIN px.categories c
                    WHERE px = p AND c.slug = :categorySlug
              ))
              AND (CAST(:search AS String) IS NULL
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
              AND (:onlyOnSale = false OR p.priceOriginal IS NOT NULL)
            """)
    Page<ProductSummary> findSummaries(
            @Param("tenantId") UUID tenantId,
            @Param("categorySlug") String categorySlug,
            @Param("search") String search,
            @Param("onlyOnSale") boolean onlyOnSale,
            Pageable pageable
    );

    /**
     * Listado del admin (issue #33, Bloque E) — a diferencia de
     * {@link #findSummaries}, no filtra {@code active} incondicionalmente: lo
     * hace opcional vía {@code incluirInativos}, y expone {@code active} y
     * {@code stock} directo (el admin necesita distinguir productos ocultos y
     * ver el estoque, cosas que la grilla pública no muestra).
     *
     * @param incluirInativos false = solo activos (default del admin); true =
     *                        activos + inactivos
     */
    @Query(value = """
            SELECT new com.frontpet.catalog.dto.AdminProductSummary(
                p.publicId,
                p.slug,
                p.nome,
                p.mainImageUrl,
                COALESCE(p.price, (
                    SELECT MIN(v.price) FROM ProductVariant v
                    WHERE v.product = p AND v.active = true
                )),
                p.priceOriginal,
                b.nome,
                CASE WHEN EXISTS (
                    SELECT 1 FROM ProductVariant v2
                    WHERE v2.product = p AND v2.active = true
                ) THEN TRUE ELSE FALSE END,
                p.active,
                p.stock
            )
            FROM Product p
            LEFT JOIN p.brand b
            WHERE p.tenantId = :tenantId
              AND (:incluirInativos = true OR p.active = true)
              AND (CAST(:categorySlug AS String) IS NULL OR EXISTS (
                    SELECT 1 FROM Product px JOIN px.categories c
                    WHERE px = p AND c.slug = :categorySlug
              ))
              AND (CAST(:search AS String) IS NULL
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND (:incluirInativos = true OR p.active = true)
              AND (CAST(:categorySlug AS String) IS NULL OR EXISTS (
                    SELECT 1 FROM Product px JOIN px.categories c
                    WHERE px = p AND c.slug = :categorySlug
              ))
              AND (CAST(:search AS String) IS NULL
                   OR LOWER(p.nome) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            """)
    Page<AdminProductSummary> findAdminSummaries(
            @Param("tenantId") UUID tenantId,
            @Param("categorySlug") String categorySlug,
            @Param("search") String search,
            @Param("incluirInativos") boolean incluirInativos,
            Pageable pageable
    );

    /**
     * Conteo para o mini-dashboard admin ({@code ProductService.countStockSummary}):
     * total de produtos ativos.
     */
    long countByTenantIdAndActiveTrue(UUID tenantId);

    /**
     * Produtos ativos com estoque efetivo zero. "Efetivo" porque
     * {@code p.stock} só vale quando o produto NÃO tem variantes (Product.stock);
     * quando tem, o que importa é se alguma variante ativa tem {@code stock>0}.
     */
    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.active = true
              AND NOT EXISTS (
                    SELECT 1 FROM ProductVariant v
                    WHERE v.product = p AND v.active = true AND v.stock > 0
              )
              AND (p.stock = 0 OR EXISTS (
                    SELECT 1 FROM ProductVariant v2
                    WHERE v2.product = p AND v2.active = true
              ))
            """)
    long countActiveOutOfStock(@Param("tenantId") UUID tenantId);

    /**
     * Detalle del producto para {@code /produtos/{slug}}.
     *
     * <p>Acá sí traemos la entidad con {@code @EntityGraph}, que carga marca,
     * variantes, categorías y especies junto con el producto. Es seguro porque
     * es <b>una sola fila</b>: sin paginación de por medio, el problema de
     * memoria del listado no existe.
     */
    @EntityGraph(attributePaths = {"brand", "variants", "categories", "species"})
    Optional<Product> findByTenantIdAndSlug(UUID tenantId, String slug);

    /** Chequeo de colisión al generar el slug (racao-golden, racao-golden-2, ...). */
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    /** Identificador interno estable — no va en URLs, lo usan carrito y pedidos. */
    @EntityGraph(attributePaths = {"brand", "variants", "categories", "species"})
    Optional<Product> findByTenantIdAndPublicId(UUID tenantId, UUID publicId);
}
