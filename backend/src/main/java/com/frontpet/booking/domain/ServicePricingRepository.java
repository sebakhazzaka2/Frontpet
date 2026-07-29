package com.frontpet.booking.domain;

import com.frontpet.booking.dto.ServicePriceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePricingRepository extends JpaRepository<ServicePricing, Long> {

    Optional<ServicePricing> findByServiceIdAndSize(Long serviceId, Porte size);

    List<ServicePricing> findByServiceIdInAndSize(List<Long> serviceIds, Porte size);

    /**
     * Tarifas del combo completo (base + adicionais) para un porte, en una
     * sola query (ADR 020, paso 3).
     *
     * <p>Devuelve también el {@code type} de cada servicio para que la capa de
     * servicio valide "el base es BASE y los adicionais son ADDON" sin una
     * segunda consulta. Filtra por {@code tenantId} y {@code active}: un id de
     * otro tenant, inactivo, o sin tarifa cargada para ese porte simplemente
     * no aparece en el resultado, y el service lo reporta como 400.
     */
    @Query("""
            SELECT new com.frontpet.booking.dto.ServicePriceLine(
                s.id, s.type, sp.price, sp.durationMinutes
            )
            FROM ServicePricing sp
            JOIN sp.service s
            WHERE s.tenantId = :tenantId
              AND s.active = true
              AND s.id IN :serviceIds
              AND sp.size = :size
            """)
    List<ServicePriceLine> findPriceLines(
            @Param("tenantId") UUID tenantId,
            @Param("serviceIds") Collection<Long> serviceIds,
            @Param("size") Porte size
    );
}
