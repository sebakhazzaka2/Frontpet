package com.frontpet.common;

import java.time.Instant;

/**
 * Cuerpo de error uniforme para toda la API.
 *
 * @param message texto para el usuario final, en PT-BR (ADR 007)
 * @param path    ruta que falló, para poder correlacionar con los logs
 */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now());
    }
}
