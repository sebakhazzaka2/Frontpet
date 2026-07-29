package com.frontpet.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicePricingRepository extends JpaRepository<ServicePricing, Long> {

    Optional<ServicePricing> findByServiceIdAndSize(Long serviceId, Porte size);

    List<ServicePricing> findByServiceIdInAndSize(List<Long> serviceIds, Porte size);
}
