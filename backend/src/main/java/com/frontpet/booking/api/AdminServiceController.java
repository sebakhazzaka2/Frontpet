package com.frontpet.booking.api;

import com.frontpet.booking.ServiceCatalogService;
import com.frontpet.booking.dto.ServiceOfferingDetail;
import com.frontpet.booking.dto.UpdateServiceRequest;
import com.frontpet.identity.domain.AdminUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin de serviços (tarea 5.7, Bloque G). Protegido por
 * {@code anyRequest().authenticated()} de {@link com.frontpet.config.SecurityConfig}
 * — mismo criterio que {@link com.frontpet.orders.api.AdminOrderController}.
 *
 * <p>A propósito no hay {@code @PostMapping}/{@code @DeleteMapping}: la lista
 * de serviços es fija en seed, el admin solo edita (ADR 009). Sin esos
 * mappings, Spring ya responde 405 a {@code POST}/{@code DELETE} sobre
 * {@code /{id}} — el AC de la issue #51 pide que ese 405 quede codificado en
 * un test, no la implementación (ver {@code AdminServiceControllerTest}).
 */
@RestController
@RequestMapping("/api/v1/admin/services")
@RequiredArgsConstructor
public class AdminServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    public List<ServiceOfferingDetail> list(@AuthenticationPrincipal AdminUser admin) {
        return serviceCatalogService.listAll(admin.getTenantId());
    }

    @PutMapping("/{id}")
    public ServiceOfferingDetail update(@AuthenticationPrincipal AdminUser admin,
                                        @PathVariable Long id,
                                        @Valid @RequestBody UpdateServiceRequest request) {
        return serviceCatalogService.update(admin.getTenantId(), id, request);
    }
}
