package com.frontpet.config;

import com.frontpet.identity.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad.
 *
 * <p>Con {@code spring-boot-starter-security} en el classpath y sin un
 * {@link SecurityFilterChain} propio, Spring bloquea absolutamente todo con
 * HTTP Basic. Es decir: los endpoints públicos del catálogo no son públicos
 * hasta que alguien lo diga acá.
 *
 * <p>Login/JWT portado de {@code consultorio-odontologico} (docs/reuse-consultorio.md
 * §1): {@link JwtAuthFilter} valida la cookie HttpOnly (ADR 004) y puebla el
 * {@code SecurityContext}; {@code /api/v1/admin/**} queda cubierto por el
 * {@code anyRequest().authenticated()} de más abajo, no necesita una regla propia.
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

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Engancha el CorsConfigurationSource de CorsConfig. Sin esta
                // línea la config de CORS existe pero Spring Security nunca la
                // aplica, y el preflight OPTIONS muere en 401 antes de llegar.
                .cors(cors -> {})
                // API stateless con JWT en cookie: el token es inmune a lectura
                // por JS (HttpOnly) y SameSite=Lax mitiga el caso común de CSRF
                // en los POST cross-site. Evaluar un token CSRF explícito en
                // Fase 2 para las escrituras del admin (ADR 004, nota de reuse-consultorio.md §1).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        // Exact-match no alcanza: con management.endpoint.health.probes
                        // habilitado (tarea 1.7), /actuator/health/liveness y /readiness
                        // — que consume el healthcheck de Coolify — caen en el
                        // anyRequest().authenticated() de abajo sin el /**.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                        // Todo lo demás (incluido /api/v1/admin/**) requiere la
                        // cookie de sesión validada por jwtAuthFilter.
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // Sin esto, deshabilitar httpBasic/formLogin deja a Spring sin
                // ningún AuthenticationEntryPoint configurado, y el default
                // (Http403ForbiddenEntryPoint) devuelve 403 para *cualquier*
                // request sin autenticar — nunca 401. Encontrado con el test
                // de la tarea 3.4 ("las 3 rutas devuelven 401 sin cookie").
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
