package com.frontpet.booking;

import com.frontpet.booking.domain.ServiceOffering;
import com.frontpet.booking.domain.ServiceOfferingRepository;
import com.frontpet.booking.domain.ServicePricing;
import com.frontpet.booking.domain.Porte;
import com.frontpet.booking.dto.ServiceOfferingDetail;
import com.frontpet.booking.dto.ServicePricingDetail;
import com.frontpet.booking.dto.ServicePricingRequest;
import com.frontpet.booking.dto.UpdateServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    @Transactional
    public ServiceOfferingDetail update(UUID tenantId, Long id, UpdateServiceRequest request) {
        ServiceOffering service = serviceOfferingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException("Serviço não encontrado."));

        service.setNome(request.nome());
        service.setDescricao(request.descricao());
        service.setActive(request.active());

        Map<Porte, ServicePricing> bySize = new EnumMap<>(Porte.class);
        for (ServicePricing pricing : service.getPricing()) {
            bySize.put(pricing.getSize(), pricing);
        }

        for (ServicePricingRequest pricingRequest : request.pricing()) {
            ServicePricing pricing = bySize.get(pricingRequest.size());
            if (pricing == null) {
                throw new IllegalArgumentException(
                        "Tarifa para o porte " + pricingRequest.size() + " não existe para este serviço.");
            }
            pricing.setPrice(pricingRequest.price());
            pricing.setDurationMinutes(pricingRequest.durationMinutes());
        }

        return toDetail(service);
    }

    private ServiceOfferingDetail toDetail(ServiceOffering service) {
        List<ServicePricingDetail> pricing = service.getPricing().stream()
                .map(p -> new ServicePricingDetail(p.getSize(), p.getPrice(), p.getDurationMinutes()))
                .toList();
        return new ServiceOfferingDetail(
                service.getId(), service.getType(), service.getNome(), service.getDescricao(),
                service.getActive(), pricing);
    }
}
