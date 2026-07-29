package com.frontpet.tenant;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.tenant.dto.BookingSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lee {@code tenants.config} tal como lo siembra {@code V5__seed_dev.sql}
 * (capacidade=2, anticipação 1h-60d — §2.2/§3.1 de preguntas-cliente.md).
 */
@SpringBootTest
@Transactional
class TenantSettingsServiceImplTest extends AbstractIntegrationTest {

    private static final UUID SEEDED_TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired TenantSettingsService tenantSettingsService;

    @Test
    @DisplayName("lee la config real sembrada por V5")
    void readsSeededConfig() {
        BookingSettings settings = tenantSettingsService.booking(SEEDED_TENANT);

        assertThat(settings.capacidadeAtendimento()).isEqualTo(2);
        assertThat(settings.anticipacaoMinHoras()).isEqualTo(1);
        assertThat(settings.anticipacaoMaxDias()).isEqualTo(60);
    }

    @Test
    @DisplayName("tenant inexistente falla explícito, no con defaults silenciosos")
    void missingTenantFails() {
        assertThatThrownBy(() -> tenantSettingsService.booking(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }
}
