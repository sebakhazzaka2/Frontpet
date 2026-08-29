package com.frontpet.identity;

import com.frontpet.identity.domain.AdminUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test puro (sin Spring context, como {@code LoginAttemptServiceTest}):
 * la invalidación de sesión vía {@code passwordChangedAt} (tarea 7.12, ADR
 * 022) toca el camino de auth de todas las requests, así que se aísla acá en
 * vez de solo cubrirse indirectamente por el flujo completo de reset (ese
 * caso de punta a punta vive en {@code PasswordResetControllerIntegrationTest}).
 *
 * <p>{@code doFilterInternal} es {@code protected}, pero este test vive en el
 * mismo paquete que {@link JwtAuthFilter} — no hace falta subclasificar para
 * invocarlo directo.
 */
class JwtAuthFilterTest {

    private static final String SECRET = "dev-only-secret-nao-usar-em-producao-32chars";
    private static final long EXPIRATION_MS = 3_600_000L; // 1h, igual al default de application.yml

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService);

    @AfterEach
    void clearSecurityContext() {
        // El SecurityContextHolder es un ThreadLocal estático: sin limpiar,
        // un test que autentica filtra el estado al siguiente.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("extractIssuedAt devuelve el iat con el que se firmó el token")
    void extractIssuedAtReturnsIssuedAt() {
        AdminUser admin = adminWithPasswordChangedAt(Instant.now().minusSeconds(60));
        String token = jwtService.generateToken(admin);

        Instant issuedAt = jwtService.extractIssuedAt(token);

        assertThat(issuedAt).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("token emitido después del último cambio de contraseña autentica normalmente")
    void tokenIssuedAfterPasswordChangeAuthenticates() throws Exception {
        AdminUser admin = adminWithPasswordChangedAt(Instant.now().minusSeconds(3600));
        when(userDetailsService.loadUserByUsername(admin.getEmail())).thenReturn(admin);
        String token = jwtService.generateToken(admin);

        runFilter(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("token emitido antes de un reset de contraseña posterior no autentica (sesión revocada)")
    void tokenIssuedBeforePasswordChangeIsRejected() throws Exception {
        // passwordChangedAt claramente después del iat del token (no es el
        // caso límite del mismo segundo, que se documenta en
        // PasswordResetControllerIntegrationTest con un Clock mockeado).
        AdminUser admin = adminWithPasswordChangedAt(Instant.now().plusSeconds(3600));
        when(userDetailsService.loadUserByUsername(admin.getEmail())).thenReturn(admin);
        String token = jwtService.generateToken(admin);

        runFilter(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void runFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SessionCookie.NAME, token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request, response, chain);
    }

    private AdminUser adminWithPasswordChangedAt(Instant passwordChangedAt) {
        AdminUser admin = new AdminUser();
        admin.setEmail("admin@frontpet.dev");
        admin.changePassword("{bcrypt}hash-de-teste", passwordChangedAt);
        admin.setRole("ADMIN");
        return admin;
    }
}
