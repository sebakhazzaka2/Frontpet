package com.frontpet.catalog;

import com.frontpet.catalog.domain.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BrandService.findOrCreate — la puerta de entrada del alta inline de marcas
 * (docs/pending-decisions.md §1).
 */
@SpringBootTest
@Transactional
class BrandServiceIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired BrandService brandService;

    @Test
    @DisplayName("crea la marca si no existe")
    void createsWhenMissing() {
        Brand brand = brandService.findOrCreate(TENANT, "Golden Nova");

        assertThat(brand.getId()).isNotNull();
        assertThat(brand.getNome()).isEqualTo("Golden Nova");
    }

    @Test
    @DisplayName("reutiliza la marca existente sin distinguir mayúsculas")
    void reusesExistingIgnoringCase() {
        Brand created = brandService.findOrCreate(TENANT, "Pedigree");

        Brand foundLower = brandService.findOrCreate(TENANT, "pedigree");
        Brand foundUpper = brandService.findOrCreate(TENANT, "PEDIGREE");

        assertThat(foundLower.getId()).isEqualTo(created.getId());
        assertThat(foundUpper.getId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("recorta espacios antes de buscar o crear")
    void trimsWhitespace() {
        Brand created = brandService.findOrCreate(TENANT, "  Royal Canin  ");
        assertThat(created.getNome()).isEqualTo("Royal Canin");

        Brand found = brandService.findOrCreate(TENANT, "Royal Canin");
        assertThat(found.getId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("nombre vacío o solo espacios es inválido")
    void rejectsBlankName() {
        assertThatThrownBy(() -> brandService.findOrCreate(TENANT, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
