package com.frontpet.orders;

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
 * Rate limit de {@code POST /api/v1/orders} por IP (tarea 4.15). Corre antes
 * de que la request llegue a {@code OrderServiceImpl}, mismo lugar en la
 * cadena que {@code identity.LoginRateLimitFilter} — ver el javadoc de
 * {@link OrderRateLimitService} para por qué no comparten código.
 *
 * <p>A diferencia del login, acá se cuenta la request <b>antes</b> de
 * ejecutar el resto de la cadena, no después mirando el status de la
 * respuesta: no hay un "éxito" que deba resetear el contador.
 */
@Component
public class OrderRateLimitFilter extends OncePerRequestFilter {

    private static final String ORDERS_PATH = "/api/v1/orders";
    private static final String MESSAGE = "Muitos pedidos em pouco tempo. Tente novamente em alguns minutos.";

    private final OrderRateLimitService rateLimit;
    private final ObjectMapper objectMapper;

    public OrderRateLimitFilter(OrderRateLimitService rateLimit, ObjectMapper objectMapper) {
        this.rateLimit = rateLimit;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && ORDERS_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String key = "ip:" + request.getRemoteAddr();

        long retryAfterSeconds = rateLimit.blockedSecondsRemaining(key);
        if (retryAfterSeconds > 0) {
            writeTooManyRequests(request, response, retryAfterSeconds);
            return;
        }

        rateLimit.recordRequest(key);
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
