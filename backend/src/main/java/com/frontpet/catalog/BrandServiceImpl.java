package com.frontpet.catalog;

import com.frontpet.catalog.domain.Brand;
import com.frontpet.catalog.domain.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandRaceSafeCreator raceSafeCreator;

    @Override
    @Transactional
    public Brand findOrCreate(UUID tenantId, String nome) {
        String trimmed = nome == null ? "" : nome.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Nome da marca não pode ser vazio.");
        }

        return brandRepository.findByTenantIdAndNomeIgnoreCase(tenantId, trimmed)
                .orElseGet(() -> raceSafeCreator.create(tenantId, trimmed));
    }
}
