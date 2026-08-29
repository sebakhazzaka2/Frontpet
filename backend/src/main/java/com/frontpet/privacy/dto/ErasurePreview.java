package com.frontpet.privacy.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/v1/admin/privacy/preview} — o que seria
 * anonimizado, ANTES de executar (a anonimização é irreversível: o telefone
 * normalizado que a identifica é exatamente o dado que se apaga).
 */
public record ErasurePreview(
        List<UUID> orderPublicIds,
        List<UUID> appointmentPublicIds
) {
}
