package com.frontpet.booking.dto;

import com.frontpet.booking.domain.ServiceType;

import java.util.List;

public record ServiceOfferingDetail(
        Long id,
        ServiceType type,
        String nome,
        String descricao,
        boolean active,
        List<ServicePricingDetail> pricing
) {
}
