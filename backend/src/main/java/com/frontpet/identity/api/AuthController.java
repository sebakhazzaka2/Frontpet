package com.frontpet.identity.api;

import com.frontpet.identity.AuthService;
import com.frontpet.identity.SessionCookie;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portado de {@code controller/AuthController.java} del consultorio
 * (docs/reuse-consultorio.md §1, 🟡): DTOs como records, {@code login} setea
 * la cookie HttpOnly en vez de devolver el token en el body (ADR 004), y
 * agrega {@code logout} (el consultorio no lo tenía porque el front borraba
 * el token del storage). Sin {@code register}: el admin se seedea.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest body) {
        String token = authService.login(body.email(), body.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(token, Duration.ofMillis(jwtExpirationMs)).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseCookie sessionCookie(String token, Duration maxAge) {
        // HttpOnly + Secure + SameSite=Lax por ADR 004. Secure siempre (incluso
        // en dev, vía HTTPS tunnels) — el ADR exige HTTPS en todos los entornos.
        return ResponseCookie.from(SessionCookie.NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record LoginRequest(String email, String password) {
    }
}
