package com.frontpet.catalog.api;

import com.frontpet.catalog.ProductService;
import com.frontpet.catalog.dto.CreateProductRequest;
import com.frontpet.catalog.dto.ProductDetail;
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
