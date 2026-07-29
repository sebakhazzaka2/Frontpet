package com.frontpet.tenant;

import com.frontpet.tenant.domain.Tenant;
import com.frontpet.tenant.domain.TenantRepository;
import com.frontpet.tenant.dto.BookingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Lee {@code tenants.config} (JSONB genérico, ADR 002) y lo tipa contra
 * {@link BookingSettings} para el módulo de booking (ADR 009/012).
 *
 * <p>Defaults conservadores si una clave falta: capacidade 1 (ADR 009), sin
 * anticipación mínima, 60 días de ventana — el seed real (V5) siempre trae
 * las tres, estos valores solo cubren un tenant mal sembrado en test.
 */
@Service
@RequiredArgsConstructor
public class TenantSettingsServiceImpl implements TenantSettingsService {

    private static final int DEFAULT_CAPACIDADE = 1;
    private static final int DEFAULT_ANTICIPACAO_MIN_HORAS = 0;
    private static final int DEFAULT_ANTICIPACAO_MAX_DIAS = 60;

    private final TenantRepository tenantRepository;

    @Override
    public BookingSettings booking(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant não encontrado: " + tenantId));
        Map<String, Object> config = tenant.getConfig();

        return new BookingSettings(
                intOrDefault(config, "capacidade_atendimento", DEFAULT_CAPACIDADE),
                intOrDefault(config, "anticipacao_min_horas", DEFAULT_ANTICIPACAO_MIN_HORAS),
                intOrDefault(config, "anticipacao_max_dias", DEFAULT_ANTICIPACAO_MAX_DIAS)
        );
    }

    private int intOrDefault(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }
}
