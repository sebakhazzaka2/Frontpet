package com.frontpet.booking.api;

import com.frontpet.booking.ServiceCatalogService;
import com.frontpet.booking.dto.ServiceOfferingDetail;
import com.frontpet.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo público de serviços (tarea 5.4). Sem autenticação, mesmo critério
 * de {@link com.frontpet.catalog.api.PublicCatalogController}: o tenant sai
 * de {@link CurrentTenant}, não há JWT do qual tirá-lo.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicServiceController {

    private final ServiceCatalogService serviceCatalogService;
    private final CurrentTenant currentTenant;

    @GetMapping("/services")
    public List<ServiceOfferingDetail> list() {
        return serviceCatalogService.listActive(currentTenant.id());
    }
}
