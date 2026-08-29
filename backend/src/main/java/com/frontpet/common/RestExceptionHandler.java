package com.frontpet.common;

import com.frontpet.booking.AppointmentNotFoundException;
import com.frontpet.booking.ScheduleBlockNotFoundException;
import com.frontpet.booking.ServiceOfferingNotFoundException;
import com.frontpet.booking.SlotUnavailableException;
import com.frontpet.catalog.ProductNotFoundException;
import com.frontpet.orders.OrderNotFoundException;
import com.frontpet.privacy.PrivacyRecordNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderNotFound(OrderNotFoundException ex,
                                                        HttpServletRequest request) {
        log.debug("Pedido não encontrado: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Pedido não encontrado.", request);
    }

    @ExceptionHandler(ServiceOfferingNotFoundException.class)
    public ResponseEntity<ApiError> handleServiceNotFound(ServiceOfferingNotFoundException ex,
                                                           HttpServletRequest request) {
        log.debug("Serviço não encontrado: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Serviço não encontrado.", request);
    }

    @ExceptionHandler(ScheduleBlockNotFoundException.class)
    public ResponseEntity<ApiError> handleScheduleBlockNotFound(ScheduleBlockNotFoundException ex,
                                                                 HttpServletRequest request) {
        log.debug("Bloqueio não encontrado: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Bloqueio não encontrado.", request);
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ApiError> handleAppointmentNotFound(AppointmentNotFoundException ex,
                                                               HttpServletRequest request) {
        log.debug("Turno não encontrado: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Turno não encontrado.", request);
    }

    @ExceptionHandler(PrivacyRecordNotFoundException.class)
    public ResponseEntity<ApiError> handlePrivacyRecordNotFound(PrivacyRecordNotFoundException ex,
                                                                 HttpServletRequest request) {
        log.debug("Registro de privacidade não encontrado: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ApiError> handleSlotUnavailable(SlotUnavailableException ex,
                                                           HttpServletRequest request) {
        // Debug, no error: perder la carrera por un cupo es tráfico normal
        // en un slot popular, no un incidente.
        log.debug("Cupo indisponível em {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex,
                                                     HttpServletRequest request) {
        log.debug("Requisição inválida em {}: {}", request.getRequestURI(), ex.getMessage());
        // Usa el mensaje real de la excepción de dominio (ej. BrandService:
        // "Nome da marca não pode ser vazio.") — antes se pisaba con un texto
        // genérico y esos mensajes claros nunca llegaban al cliente.
        String message = ex.getMessage() != null ? ex.getMessage() : "Requisição inválida.";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        // Sin este handler, un @Valid fallido sale con el formato de error por
        // defecto de Spring, no con nuestro ApiError — inconsistente para el
        // frontend, que espera la misma forma en todos los 4xx.
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        log.debug("Validação falhou em {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(ApiError.ofValidation(request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                                                          HttpServletRequest request) {
        // Debug, no error: un login fallido es tráfico normal, no un incidente.
        log.debug("Falha de autenticação em {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos.", request);
    }

    // Red de seguridad final: cualquier excepción sin handler específico
    // llegaba antes como 500 crudo de Spring (con stacktrace en el body en
    // dev). Acá se loguea completo para Sentry/logs, pero al cliente solo
    // llega un mensaje genérico en PT-BR.
    //
    // Excepciones propias de Spring MVC (405 método no permitido, 415 media
    // type, 404 de ruta inexistente, etc.) implementan ErrorResponse y ya
    // traen su status HTTP correcto — no son un bug real, así que se
    // respeta ese status en vez de aplastarlo con un 500 (AdminServiceControllerTest
    // cubre el caso 405 de "admin não cria/apaga serviços").
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.debug("Erro HTTP padrão do Spring em {}: {}", request.getRequestURI(), ex.getMessage());
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(status).body(ApiError.of(
                    status.value(),
                    HttpStatus.valueOf(status.value()).getReasonPhrase(),
                    detail != null ? detail : "Requisição inválida.",
                    request.getRequestURI()));
        }
        log.error("Erro não tratado em {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente.", request);
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
