package com.frontpet.privacy.dto;

import jakarta.validation.constraints.NotBlank;

/** Body de {@code POST /api/v1/admin/privacy/anonymize}. */
public record ErasureRequest(
        @NotBlank String clienteTelefone
) {
}
