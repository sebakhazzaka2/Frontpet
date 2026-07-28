package com.frontpet.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontpet.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit del login por IP (tarea 1.8, ADR 019). Corre <b>antes</b> de que
 * la cadena de filtros llegue a {@code AuthService} — y por lo tanto antes de
 * que corra BCrypt (~100 ms de CPU por intento a propósito), que es lo que
 * convierte a un login sin límite en un vector de DoS contra todo el server,
 * no solo un riesgo de fuerza bruta.
 *
 * <p>Solo lee {@code request.getRemoteAddr()}, nunca {@code X-Forwarded-For}
 * a mano — ver {@code server.forward-headers-strategy} en
 * {@code application.yml} y el ADR 019.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String MESSAGE = "Muitas tentativas de login. Tente novamente em alguns minutos.";

    private final LoginAttemptService loginAttempts;
    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(LoginAttemptService loginAttempts, ObjectMapper objectMapper) {
        this.loginAttempts = loginAttempts;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Solo el POST del login. Con este guard da igual que Boot lo
        // auto-registre además en la cadena de servlets (como JwtAuthFilter):
        // fuera de esta ruta es un no-op.
        return !("POST".equals(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String key = "ip:" + request.getRemoteAddr();

        long retryAfterSeconds = loginAttempts.blockedSecondsRemaining(key);
        if (retryAfterSeconds > 0) {
            writeTooManyRequests(request, response, retryAfterSeconds);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.OK.value()) {
            loginAttempts.reset(key);
        } else if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            loginAttempts.recordFailure(key);
        }
        // Otros códigos (400 por validación, por ejemplo) no consumen
        // presupuesto: no fueron un intento real de autenticarse.
    }

    private void writeTooManyRequests(
            HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // ObjectMapper inyectado (el de Boot, con JavaTimeModule ya
        // registrado), no uno nuevo: así el Instant de ApiError sale en
        // ISO-8601 igual que desde RestExceptionHandler, que corre dentro del
        // DispatcherServlet y este filtro no puede alcanzar.
        objectMapper.writeValue(
                response.getWriter(),
                ApiError.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        MESSAGE,
                        request.getRequestURI()));
    }
}
