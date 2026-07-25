package com.frontpet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad — <b>base parcial</b>.
 *
 * <p>Con {@code spring-boot-starter-security} en el classpath y sin un
 * {@link SecurityFilterChain} propio, Spring bloquea absolutamente todo con
 * HTTP Basic. Es decir: los endpoints públicos del catálogo no son públicos
 * hasta que alguien lo diga acá.
 *
 * <p><b>Lo que falta (tarea 1.3 del ROADMAP)</b>: login con JWT en cookie
 * HttpOnly, el filtro que lo valida, y las reglas de {@code /api/v1/admin/**}.
 * Hoy todo lo no listado como público simplemente se rechaza — que es el
 * default seguro mientras no exista el login.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Rutas de lectura del catálogo público: las consume la web sin login. */
    private static final String[] PUBLIC_GET = {
            "/api/v1/products",
            "/api/v1/products/**",
            "/api/v1/categories",
            "/api/v1/species"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // API stateless: no hay sesión de servidor que proteger con CSRF.
                // ⚠️ Cuando entre el JWT en cookie (1.3) esto hay que revisarlo:
                // con cookies, SameSite=Lax cubre el caso común, pero las
                // operaciones de escritura del admin merecen una segunda mirada.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Todo lo demás cerrado. Cuando exista el login, acá se
                        // agregan las reglas de /api/v1/admin/**.
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }
}
