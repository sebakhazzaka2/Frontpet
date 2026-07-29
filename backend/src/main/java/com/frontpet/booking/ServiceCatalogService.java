package com.frontpet.booking;

import com.frontpet.booking.dto.ServiceOfferingDetail;
import com.frontpet.booking.dto.UpdateServiceRequest;

import java.util.UUID;

/**
 * Puerta de entrada admin al catálogo de serviços (banhos base + adicionais,
 * ADR 011). Solo edición — el admin nunca crea ni borra filas, la lista es
 * fija en seed (ADR 009).
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
}
