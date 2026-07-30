package com.frontpet.booking;

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
 * Rate limit de {@code POST /api/v1/appointments} por IP (tarea 5.8). Mismo
 * lugar en la cadena que {@code OrderRateLimitFilter}: cuenta la request
 * antes de ejecutar el resto de la cadena, no hay "éxito" que resetee.
 */
@Component
public class AppointmentRateLimitFilter extends OncePerRequestFilter {

    private static final String APPOINTMENTS_PATH = "/api/v1/appointments";
    private static final String MESSAGE = "Muitas reservas em pouco tempo. Tente novamente em alguns minutos.";

    private final AppointmentRateLimitService rateLimit;
    private final ObjectMapper objectMapper;

    public AppointmentRateLimitFilter(AppointmentRateLimitService rateLimit, ObjectMapper objectMapper) {
        this.rateLimit = rateLimit;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && APPOINTMENTS_PATH.equals(request.getRequestURI()));
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
