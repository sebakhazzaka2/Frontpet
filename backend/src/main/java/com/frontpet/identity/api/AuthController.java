package com.frontpet.identity.api;

import com.frontpet.identity.AuthService;
import com.frontpet.identity.PasswordResetService;
import com.frontpet.identity.SessionCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
 *
 * <p>{@code forgot-password}/{@code reset-password} (tarea 7.12, ADR 022) se
 * agregan acá y no en un controller propio: comparten el mismo helper
 * {@code sessionCookie} y la misma superficie de auth pública.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    // Vacío en dev (front/back comparten host `localhost`, distinto puerto).
    // En producción, `frontpet.com.br` — ver comentario largo en application.yml.
    @Value("${frontpet.cookie.domain}")
    private String cookieDomain;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest body) {
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

    // Siempre 202 con body vacío: email existente, inexistente o bloqueado
    // por rate limit de email son indistinguibles desde afuera (anti-enumeração,
    // ADR 022). Las únicas respuestas distintas son el 400 de validación de
    // formato (no depende de la DB) y el 429 del filtro de IP (depende de la
    // IP, no del email).
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest body, HttpServletRequest request) {
        passwordResetService.requestReset(body.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    // Set-Cookie vacío no es cosmético: el guard de admin/(protected)/layout.tsx
    // solo mira cookieStore.has(...), así que dejar la cookie vieja puesta
    // dejaría pasar al browser para después rebotar en el primer request real.
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        passwordResetService.resetPassword(body.token(), body.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseCookie sessionCookie(String token, Duration maxAge) {
        // HttpOnly + Secure + SameSite=Lax por ADR 004. Secure siempre (incluso
        // en dev, vía HTTPS tunnels) — el ADR exige HTTPS en todos los entornos.
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(SessionCookie.NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge);
        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    public record LoginRequest(
            @NotBlank(message = "Email é obrigatório.") String email,
            @NotBlank(message = "Senha é obrigatória.") String password) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "E-mail é obrigatório.")
            @Email(message = "Informe um e-mail válido.")
            String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Token é obrigatório.") String token,
            // max = 72: BCrypt trunca silenciosamente a 72 bytes. Sem este
            // limite, duas senhas que compartilham os primeiros 72
            // caracteres seriam a mesma senha para o sistema.
            @NotBlank(message = "Senha é obrigatória.")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
            String password) {
    }
}
