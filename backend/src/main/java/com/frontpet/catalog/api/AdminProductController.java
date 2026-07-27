package com.frontpet.catalog.api;

import com.frontpet.catalog.ProductService;
import com.frontpet.catalog.R2StorageService;
import com.frontpet.catalog.dto.CreateProductRequest;
import com.frontpet.catalog.dto.PresignedUploadRequest;
import com.frontpet.catalog.dto.PresignedUploadResponse;
import com.frontpet.catalog.dto.ProductDetail;
import com.frontpet.catalog.dto.ProductVariantUpsertRequest;
import com.frontpet.catalog.dto.UpdateProductRequest;
import com.frontpet.identity.domain.AdminUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * tiene {@code ON DELETE RESTRICT} (ADR 013 §12).
     */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AdminUser admin,
                                           @PathVariable UUID publicId) {
        productService.setActive(admin.getTenantId(), publicId, false);
        return ResponseEntity.noContent().build();
    }
}
