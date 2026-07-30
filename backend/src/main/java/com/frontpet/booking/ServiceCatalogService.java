package com.frontpet.booking;

import com.frontpet.booking.dto.ServiceOfferingDetail;
import com.frontpet.booking.dto.UpdateServiceRequest;

import java.util.List;
import java.util.UUID;

/**
 * Puerta de entrada al catálogo de serviços (banhos base + adicionais,
 * ADR 011): edición para el admin, listado para el público (5.4).
 */
public interface ServiceCatalogService {

    /**
     * Edita nome/descricao/active y las tarifas por porte de un serviço
     * existente.
     *
     * @throws ServiceOfferingNotFoundException se o serviço não existe ou não
     *                                           pertence ao tenant
     * @throws IllegalArgumentException         se alguma entrada de {@code pricing}
     *                                           referencia um porte que este
     *                                           serviço não tem sembrado
     */
    ServiceOfferingDetail update(UUID tenantId, Long id, UpdateServiceRequest request);

    /**
     * Catálogo público — solo {@code active = true} (contrato en
     * {@code docs/booking-api-contracts.md}, sección {@code GET /api/v1/services}).
     */
    List<ServiceOfferingDetail> listActive(UUID tenantId);
}
