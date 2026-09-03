package com.frontpet.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY — verificación end-to-end de Sentry en producción (D.8 del
 * deploy-runbook). Un solo GET público que revienta a propósito para
 * confirmar que el error llega al dashboard de Sentry. Borrar este archivo
 * entero (y su matcher en {@link SecurityConfig#PUBLIC_GET}) apenas se
 * verifique; buscar "TEMPORARY" + "D.8" para encontrar todo lo relacionado.
 */
@RestController
public class TemporarySentryTestController {

    @GetMapping("/api/v1/_sentry-test")
    public void throwForSentry() {
        throw new RuntimeException("TEMPORARY — Sentry D.8 verification, safe to delete this whole file");
    }
}
