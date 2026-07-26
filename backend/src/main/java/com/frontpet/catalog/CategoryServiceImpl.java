package com.frontpet.catalog;

import com.frontpet.catalog.domain.Category;
import com.frontpet.catalog.domain.CategoryRepository;
import com.frontpet.catalog.domain.SpeciesRepository;
import com.frontpet.catalog.dto.TaxonRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SpeciesRepository speciesRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaxonRef> listCategories(UUID tenantId) {
        return categoryRepository.findByTenantId(tenantId).stream()
                .map(this::toRef)
                .sorted(Comparator.comparing(TaxonRef::nome))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxonRef> listSpecies(UUID tenantId) {
        return speciesRepository.findByTenantId(tenantId).stream()
                .map(s -> new TaxonRef(s.getNome(), s.getSlug()))
                .sorted(Comparator.comparing(TaxonRef::nome))
                .toList();
    }

    private TaxonRef toRef(Category category) {
        return new TaxonRef(category.getNome(), category.getSlug());
    }
}
