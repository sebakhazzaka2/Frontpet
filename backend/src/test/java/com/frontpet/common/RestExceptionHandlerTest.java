package com.frontpet.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Red de seguridad final de {@code RestExceptionHandler}: sin
 * {@code handleUnexpected}, una excepción sin handler específico (ej. un
 * {@code NullPointerException} de un bug real) salía como 500 crudo de
 * Spring, exponiendo el stacktrace en el body en dev.
 */
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void unmappedExceptionReturns500WithGenericMessageNoStacktrace() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/qualquer-coisa");
        RuntimeException ex = new RuntimeException("detalhe interno sensível: token-conexao-db-9f3a1c");

        ResponseEntity<ApiError> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).doesNotContain("token-conexao-db-9f3a1c");
        assertThat(body.message()).doesNotContain(ex.getClass().getName());
        assertThat(body.path()).isEqualTo("/api/v1/qualquer-coisa");
    }

    @Test
    void springMvcErrorResponseKeepsItsOwnStatusInsteadOf500() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/admin/services/1");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ApiError> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
