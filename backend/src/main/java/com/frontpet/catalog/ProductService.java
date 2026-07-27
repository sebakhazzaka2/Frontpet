package com.frontpet.catalog;

import com.frontpet.catalog.dto.CreateProductRequest;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductSummary;
import com.frontpet.catalog.dto.ProductVariantUpsertRequest;
import com.frontpet.catalog.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Puerta de entrada al módulo catalog.
 *
 * <p>Los demás módulos hablan con esta interfaz, nunca con
 * {@code ProductRepository} — por eso el repositorio vive en el sub-paquete
 * {@code domain} y esto está en la raíz del módulo (CLAUDE.md §5).
 *
 * <p>Todo método recibe {@code tenantId} explícito. Hoy hay un solo tenant,
 * pero pasarlo como parámetro desde el principio evita tener que reescribir
 * las firmas cuando el tenant salga del JWT.
 */
public interface ProductService {

    /**
     * Listado del catálogo con filtros opcionales combinables.
     *
     * @param categorySlug null = todas las categorías
     * @param search       null = sin búsqueda por nombre
     * @param onlyOnSale   true = solo productos con precio tachado
     */
    Page<ProductSummary> list(UUID tenantId,
                              String categorySlug,
                              String search,
                              boolean onlyOnSale,
                              Pageable pageable);

    /** Detalle para {@code /produtos/{slug}}. Lanza si no existe o está inactivo. */
    ProductDetail getBySlug(UUID tenantId, String slug);

    /** Detalle por identificador interno. Lo usan carrito y pedidos. */
    ProductDetail getByPublicId(UUID tenantId, UUID publicId);

    /**
     * Muestra u oculta el producto del catálogo público.
     *
     * <p>Es el mecanismo de "baja" de MVP1: no hay borrado real. Un producto
     * que alguna vez fue pedido no se puede borrar — {@code order_items} tiene
     * {@code ON DELETE RESTRICT} (ADR 013 §12). Ver docs/pending-decisions.md.
     */
    void setActive(UUID tenantId, UUID publicId, boolean active);

    /**
     * Genera un slug libre a partir del nombre, agregando sufijo si hace falta:
     * {@code racao-golden}, {@code racao-golden-2}, {@code racao-golden-3}...
     *
     * <p>Público porque el alta de productos (Sprint 4) lo va a necesitar antes
     * de construir la entidad.
     */
    String generateUniqueSlug(UUID tenantId, String nome);

    /**
     * Alta de producto, con o sin variantes (tarea 3.4).
     *
     * @throws IllegalArgumentException si la invariante precio/variantes no se
     *                                  cumple (ADR 013 §2), o si algún slug de
     *                                  categoría/espécie no existe
     */
    ProductDetail create(UUID tenantId, CreateProductRequest request);

    /**
     * Edición de producto: reemplazo completo de los campos editables, salvo
     * variantes (docs/pending-decisions.md §5 — no se tocan acá) y salvo
     * {@code slug} en blanco (significa "no tocar", nunca "vaciar").
     *
     * @throws ProductNotFoundException si no existe
     * @throws IllegalArgumentException si la invariante precio/variantes no se
     *                                  cumple, si el slug nuevo colisiona con
     *                                  otro producto, o si algún slug de
     *                                  categoría/espécie no existe
     */
    ProductDetail update(UUID tenantId, UUID publicId, UpdateProductRequest request);

    /**
     * Reemplaza el conjunto de variantes de un producto (tarea 3.4b —
     * docs/pending-decisions.md §5): upsert por id + soft-delete de las que
     * faltan en el request. Nunca hard-delete — {@code order_items} tiene FK
     * real a {@code product_variants} ({@code ON DELETE RESTRICT}).
     *
     * <p>Solo funciona si el producto YA tiene variantes: convertir un
     * producto de precio simple a variantes (o al revés) es una transición
     * de modo aparte, no la resuelve este método.
     *
     * @throws ProductNotFoundException si no existe
     * @throws IllegalArgumentException si el producto no tiene variantes, si
     *                                  algún id no pertenece a este producto,
     *                                  o si el resultado deja el producto sin
     *                                  ninguna variante activa
     */
    ProductDetail replaceVariants(UUID tenantId, UUID publicId, List<ProductVariantUpsertRequest> variants);
}
