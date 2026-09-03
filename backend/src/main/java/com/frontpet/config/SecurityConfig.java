package com.frontpet.config;

import com.frontpet.booking.AppointmentRateLimitFilter;
import com.frontpet.identity.JwtAuthFilter;
import com.frontpet.identity.LoginRateLimitFilter;
import com.frontpet.identity.PasswordResetRateLimitFilter;
import com.frontpet.orders.OrderRateLimitFilter;
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
            "/api/v1/species",
            "/api/v1/services",
            "/api/v1/availability",
            "/api/v1/appointments/**"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final OrderRateLimitFilter orderRateLimitFilter;
    private final AppointmentRateLimitFilter appointmentRateLimitFilter;
    private final PasswordResetRateLimitFilter passwordResetRateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, LoginRateLimitFilter loginRateLimitFilter,
                          OrderRateLimitFilter orderRateLimitFilter,
                          AppointmentRateLimitFilter appointmentRateLimitFilter,
                          PasswordResetRateLimitFilter passwordResetRateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.orderRateLimitFilter = orderRateLimitFilter;
        this.appointmentRateLimitFilter = appointmentRateLimitFilter;
        this.passwordResetRateLimitFilter = passwordResetRateLimitFilter;
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
                // Headers de seguridad estándar (D.9, checklist pre-lanzamiento):
                // API JSON-only que nunca debería embeberse en un iframe, y HSTS
                // solo aplica sobre requests HTTPS (correcto: prod es HTTPS-only,
                // dev queda sin el header).
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        // Exact-match no alcanza: con management.endpoint.health.probes
                        // habilitado (tarea 1.7), /actuator/health/liveness y /readiness
                        // — que consume el healthcheck de Coolify — caen en el
                        // anyRequest().authenticated() de abajo sin el /**.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                        // Reset de contraseña (tarea 7.12): anónimo por diseño, igual
                        // que login. CSRF sigue deshabilitado más abajo — ambos son
                        // anónimos y reset-password exige un token de 256 bits que el
                        // atacante no tiene, no una cookie de sesión que falsificar.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password")
                        .permitAll()
                        // Checkout público y anónimo (tarea 4.8). POST-específico
                        // y ruta exacta: /api/v1/admin/orders sigue cayendo en el
                        // anyRequest().authenticated() de abajo. Protegido en
                        // cambio por orderRateLimitFilter + honeypot (tarea 4.15).
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").permitAll()
                        // Reserva pública y anónima (tarea 5.5), protegida en
                        // cambio por appointmentRateLimitFilter + honeypot,
                        // mismo criterio que /api/v1/orders.
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments").permitAll()
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
                // Orden relativo a jwtAuthFilter irrelevante: shouldNotFilter
                // lo acota a POST /api/v1/auth/login, ruta disjunta de las que
                // jwtAuthFilter le importan.
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(orderRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(appointmentRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(passwordResetRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
