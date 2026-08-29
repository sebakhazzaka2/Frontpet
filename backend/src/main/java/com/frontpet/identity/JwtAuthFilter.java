package com.frontpet.identity;

import com.frontpet.identity.domain.AdminUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Portado de {@code security/JwtAuthFilter.java} del consultorio
 * (docs/reuse-consultorio.md §1, 🟡). Único cambio real: el token viaja en la
 * cookie {@link SessionCookie#NAME} (ADR 004), no en el header
 * {@code Authorization: Bearer}. El resto — validar token, poblar el
 * {@code SecurityContext} — queda igual.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        final String jwt = extractTokenFromCookie(request);
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            final String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails) && sessionNotRevoked(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Token inválido o expirado: continuar sin autenticación.
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Invalidación de sesión al cambiar la contraseña (tarea 7.12, ADR 022).
     * Sin esto, un reset de contraseña no revoca las cookies ya emitidas: un
     * atacante con una sesión robada sobreviviría al reset, que es
     * justamente el ataque que esta feature existe para cortar.
     *
     * <p>La comparación vive acá y no en {@code JwtService} a propósito:
     * {@code JwtService} no conoce el dominio ({@code isTokenValid} es una
     * firma genérica de Spring Security) y este filtro ya tiene el
     * {@link AdminUser} concreto que devolvió {@code UserDetailsServiceImpl}.
     *
     * <p>jjwt serializa {@code iat} como NumericDate — granularidad de
     * segundo — mientras que {@code passwordChangedAt} tiene sub-segundo.
     * Dentro del mismo segundo esa información de desempate ya se perdió al
     * firmar, así que se elige fallar del lado de <b>invalidar</b>: se
     * rechaza todo token con {@code iat < floor(passwordChangedAt) + 1s}. Un
     * falso positivo cuesta un re-login de 3 segundos (y en la práctica ni
     * eso: el flujo de reset ya termina en la pantalla de login). Un falso
     * negativo dejaría viva la sesión que el reset debía matar.
     */
    private boolean sessionNotRevoked(String token, UserDetails userDetails) {
        if (!(userDetails instanceof AdminUser adminUser)) {
            return true;
        }
        Instant issuedAt = jwtService.extractIssuedAt(token);
        Instant revokedBefore = adminUser.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        return !issuedAt.isBefore(revokedBefore);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SessionCookie.NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
