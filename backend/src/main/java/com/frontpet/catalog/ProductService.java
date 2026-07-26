package com.frontpet.catalog;

import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
