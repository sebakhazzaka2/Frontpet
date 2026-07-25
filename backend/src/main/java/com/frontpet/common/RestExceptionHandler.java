package com.frontpet.common;

import com.frontpet.catalog.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones del dominio a respuestas HTTP.
 *
 * <p>Sin esto, un {@code ProductNotFoundException} sale como 500 y el frontend
 * no puede distinguir "no existe" de "se rompió el servidor".
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ProductNotFoundException ex,
                                                   HttpServletRequest request) {
        // Nivel debug: un 404 es operación normal, no un incidente. Loguearlo
        // como error llenaría Sentry de ruido cada vez que un bot pide una URL vieja.
        log.debug("Produto não encontrado: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Produto não encontrado.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex,
                                                     HttpServletRequest request) {
        log.debug("Requisição inválida em {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Requisição inválida.", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()));
    }
}
