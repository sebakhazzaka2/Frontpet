package com.frontpet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS para que el frontend en otro origen pueda llamar a la API.
 *
 * <p>En desarrollo son dos puertos distintos ({@code :3000} y {@code :8080}),
 * o sea dos orígenes distintos: sin esto el navegador bloquea toda respuesta,
 * aunque el backend conteste perfecto. En producción van a compartir dominio,
 * pero el origen sigue configurándose por si el frontend se mueve.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${frontpet.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes explícitos, nunca "*": con allowCredentials el estándar
        // prohíbe el comodín, y además el JWT va a viajar en cookie.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // Necesario para que el navegador mande la cookie HttpOnly del login
        // (tarea 1.3). Sin esto, el admin no podría autenticarse cross-origin.
        config.setAllowCredentials(true);

        // Cachea el preflight 1 h: sin esto el navegador manda un OPTIONS extra
        // antes de cada request.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
