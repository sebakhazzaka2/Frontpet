package com.frontpet.catalog;

import com.frontpet.catalog.domain.Brand;
import com.frontpet.catalog.domain.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Colaborador de {@link BrandServiceImpl} para el único momento que necesita
 * su propia transacción: el INSERT de una marca nueva.
 *
 * <p>Es un bean aparte y no un método privado de {@code BrandServiceImpl} a
 * propósito: {@code @Transactional} funciona vía proxy, y un proxy de Spring
 * no intercepta una llamada que un método se hace a sí mismo dentro de la
 * misma clase ({@code this.metodo(...)}). Puesto ahí, {@code REQUIRES_NEW}
 * sería una anotación decorativa que nunca se activa.
 */
@Component
@RequiredArgsConstructor
class BrandRaceSafeCreator {

    private final BrandRepository brandRepository;

    /**
     * Transacción propia: si dos altas de producto concurrentes ven "no
     * existe" para el mismo nombre de marca, las dos intentan crearla y
     * chocan en el {@code INSERT} contra el índice único de V9. Sin aislar
     * esto en su propia transacción, esa violación deja la transacción
     * *externa* del alta de producto marcada para rollback-only, y el
     * producto entero se pierde por una carrera en un dato secundario.
     *
     * <p>Con {@code REQUIRES_NEW}, si el INSERT falla, solo aborta esta
     * transacción chica: quien perdió la carrera puede recuperarse leyendo la
     * marca que el otro ya creó.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Brand create(UUID tenantId, String nome) {
        try {
            Brand brand = new Brand();
            brand.setTenantId(tenantId);
            brand.setNome(nome);
            return brandRepository.saveAndFlush(brand);
        } catch (DataIntegrityViolationException raceLost) {
            return brandRepository.findByTenantIdAndNomeIgnoreCase(tenantId, nome)
                    .orElseThrow(() -> raceLost);
        }
    }
}
