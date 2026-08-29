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
 * Rate limit por IP de {@code POST /forgot-password} y {@code /reset-password}
 * (tarea 7.12). Copia estructural de {@code orders.OrderRateLimitFilter}, no
 * de {@code identity.LoginRateLimitFilter}: cuenta TODA request, exitosa o
 * no — {@code forgot-password} siempre devuelve 202, "fallida" no es una
 * señal observable desde afuera (mismo criterio que {@code OrderRateLimitService}).
 *
 * <p>Cubre las DOS rutas con el mismo balde: además de proteger el server,
 * este filtro es la defensa contra fuerza bruta del token opaco de 43 chars
 * en {@code /reset-password} (probarlos por HTTP es la única forma de
 * atacarlo, ya que no se persiste en claro).
 *
 * <p>Clave {@code "ip:" + request.getRemoteAddr()}, nunca
 * {@code X-Forwarded-For} a mano (ADR 019). El {@code ObjectMapper} para el
 * cuerpo del 429 va inyectado porque el filtro corre fuera del
 * {@code DispatcherServlet} — {@code RestExceptionHandler} no lo alcanza.
 */
@Component
public class PasswordResetRateLimitFilter extends OncePerRequestFilter {

    private static final String FORGOT_PASSWORD_PATH = "/api/v1/auth/forgot-password";
    private static final String RESET_PASSWORD_PATH = "/api/v1/auth/reset-password";
    private static final String MESSAGE = "Muitas tentativas. Tente novamente em alguns minutos.";

    private final PasswordResetIpAttemptService ipAttempts;
    private final ObjectMapper objectMapper;

    public PasswordResetRateLimitFilter(PasswordResetIpAttemptService ipAttempts, ObjectMapper objectMapper) {
        this.ipAttempts = ipAttempts;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !(FORGOT_PASSWORD_PATH.equals(uri) || RESET_PASSWORD_PATH.equals(uri));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String key = "ip:" + request.getRemoteAddr();

        long retryAfterSeconds = ipAttempts.blockedSecondsRemaining(key);
        if (retryAfterSeconds > 0) {
            writeTooManyRequests(request, response, retryAfterSeconds);
            return;
        }

        ipAttempts.recordAttempt(key);
        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(
            HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ApiError.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        MESSAGE,
                        request.getRequestURI()));
    }
}
