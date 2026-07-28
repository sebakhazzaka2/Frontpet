package com.frontpet.common;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo de error uniforme para toda la API.
 *
 * @param message     texto para el usuario final, en PT-BR (ADR 007)
 * @param path        ruta que falló, para poder correlacionar con los logs
 * @param fieldErrors nombre de campo → mensaje, solo en errores de validación
 *                     ({@code @Valid} fallido). {@code null} en el resto.
 */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now(), null);
    }

    public static ApiError ofValidation(String path, Map<String, String> fieldErrors) {
        return new ApiError(400, "Bad Request", "Dados inválidos.", path, Instant.now(), fieldErrors);
    }
}
