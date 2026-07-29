package com.frontpet.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Edición de un {@code ServiceOffering} (tarea 5.7, ADR 009). Reemplazo
 * completo — mismo criterio que {@code UpdateProductRequest}: el admin edita,
 * nunca crea/borra filas de {@code pricing}, así que cada entrada debe
 * corresponder a un porte que el servicio ya tiene sembrado.
 */
public record UpdateServiceRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull Boolean active,
        @NotEmpty @Valid List<ServicePricingRequest> pricing
) {
}
