package com.frontpet.catalog.api;

import com.frontpet.catalog.ProductService;
import com.frontpet.catalog.R2StorageService;
import com.frontpet.catalog.dto.AdminProductSummary;
import com.frontpet.catalog.dto.CreateProductRequest;
import com.frontpet.catalog.dto.PresignedUploadRequest;
import com.frontpet.catalog.dto.PresignedUploadResponse;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductVariantUpsertRequest;
import com.frontpet.catalog.dto.UpdateProductRequest;
import com.frontpet.common.PageResponse;
import com.frontpet.identity.domain.AdminUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * CRUD admin de productos (tarea 3.4). Protegido por
 * {@code anyRequest().authenticated()} de {@link com.frontpet.config.SecurityConfig}
 * — no necesita una regla propia, cualquier ruta no listada como pública ya
 * exige la cookie JWT válida.
 *
 * <p>El {@code tenantId} sale de {@link AdminUser} (el principal autenticado),
 * no de {@code CurrentTenant} como los endpoints públicos. Hoy da lo mismo
 * (single-tenant), pero un endpoint de escritura que confía en config en vez
 * de en quién inició sesión es, por construcción, un agujero el día que haya
 * más de un tenant: el admin del tenant A podría escribir en el B.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final R2StorageService r2StorageService;

    /**
     * Listado admin (issue #33, Bloque E) — no existía ningún {@code GET} acá
     * hasta este bloque, solo el de {@link com.frontpet.catalog.api.PublicCatalogController},
     * que fuerza {@code active = true} y no sirve para gestionar el catálogo
     * completo (incluidos los ocultos).
     */
    @GetMapping
    public PageResponse<AdminProductSummary> list(@AuthenticationPrincipal AdminUser admin,
                                                   @RequestParam(name = "categoria", required = false) String categorySlug,
                                                   @RequestParam(name = "busca", required = false) String search,
                                                   @RequestParam(name = "incluirInativos", defaultValue = "false") boolean incluirInativos,
                                                   @PageableDefault(size = 24) Pageable pageable) {
        return PageResponse.of(productService.listAdmin(
                admin.getTenantId(), categorySlug, search, incluirInativos, pageable));
    }

    /**
     * Detalle para abrir el form de edición (issue #33) — a diferencia de
     * {@code GET /api/v1/products/{slug}} (público), no filtra {@code active}:
     * el admin necesita poder editar un producto que ocultó.
     */
    @GetMapping("/{publicId}")
    public ProductDetail get(@AuthenticationPrincipal AdminUser admin, @PathVariable UUID publicId) {
        return productService.getByPublicIdForAdmin(admin.getTenantId(), publicId);
    }

    /**
     * Firma una URL de subida directa a R2 (tarea 3.5). El admin sube la
     * foto con un PUT a {@code uploadUrl} y recién después manda
     * {@code publicUrl} como {@code mainImageUrl} en el alta/edición del
     * producto — este endpoint no toca ningún producto.
     */
    @PostMapping("/images/presign")
    public PresignedUploadResponse presignImageUpload(@AuthenticationPrincipal AdminUser admin,
                                                       @Valid @RequestBody PresignedUploadRequest request) {
        return r2StorageService.presignProductImageUpload(admin.getTenantId(), request);
    }

    @PostMapping
    public ResponseEntity<ProductDetail> create(@AuthenticationPrincipal AdminUser admin,
                                                @Valid @RequestBody CreateProductRequest request) {
        ProductDetail created = productService.create(admin.getTenantId(), request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{publicId}")
    public ProductDetail update(@AuthenticationPrincipal AdminUser admin,
                                @PathVariable UUID publicId,
                                @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(admin.getTenantId(), publicId, request);
    }

    /**
     * Upsert + soft-delete de variantes (tarea 3.4b —
     * docs/pending-decisions.md §5). Solo funciona sobre un producto que ya
     * tiene variantes; ver la nota en {@link com.frontpet.catalog.ProductService#replaceVariants}.
     */
    @PutMapping("/{publicId}/variants")
    public ProductDetail replaceVariants(@AuthenticationPrincipal AdminUser admin,
                                         @PathVariable UUID publicId,
                                         @Valid @RequestBody List<ProductVariantUpsertRequest> variants) {
        return productService.replaceVariants(admin.getTenantId(), publicId, variants);
    }

    /**
     * Soft delete ({@code active=false}) — nunca borrado físico. Un producto
     * que alguna vez fue pedido no se puede borrar de verdad: {@code order_items}
     * tiene {@code ON DELETE RESTRICT} (ADR 013 §12). Atajo semántico de
     * {@code PUT .../active} de acá abajo, para el botón "borrar" de una fila
     * de la tabla — no hace falta mandar el body para ese caso puntual.
     */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AdminUser admin,
                                           @PathVariable UUID publicId) {
        productService.setActive(admin.getTenantId(), publicId, false);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle activo/inactivo (issue #33) — {@code DeleteMapping} de arriba
     * solo apagaba; sin esto no había forma de REACTIVAR un producto oculto.
     * Es el que usa el toggle "Produto ativo" del form de edición.
     */
    @PutMapping("/{publicId}/active")
    public ResponseEntity<Void> setActive(@AuthenticationPrincipal AdminUser admin,
                                          @PathVariable UUID publicId,
                                          @Valid @RequestBody SetActiveRequest request) {
        productService.setActive(admin.getTenantId(), publicId, request.active());
        return ResponseEntity.noContent().build();
    }

    public record SetActiveRequest(boolean active) {
    }
}
