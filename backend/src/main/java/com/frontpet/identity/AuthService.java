package com.frontpet.identity;

import com.frontpet.identity.domain.AdminUser;
import com.frontpet.identity.domain.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Portado de {@code service/AuthService.java} del consultorio
 * (docs/reuse-consultorio.md §1, 🟡).
 *
 * <p>Cambios respecto al original: sin {@code register} (el admin se seedea,
 * no se autoregistra — ver {@code config/DataInitializer}), y las credenciales
 * inválidas lanzan {@link BadCredentialsException} en vez de
 * {@code RuntimeException} para que mapeen a 401 y no a 500
 * ({@link com.frontpet.common.RestExceptionHandler}).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AdminUserRepository adminUserRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** @return el JWT firmado; quien setea la cookie es {@code AuthController}. */
    public String login(String email, String password) {
        log.info("Tentativa de login — usuário: {}", email);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException e) {
            // Antes era catch (Exception e): tapaba también fallas ajenas al
            // login (p. ej. una caída de DB) como si fueran "contraseña
            // incorrecta" (tarea 1.8). El rate limit vive en
            // LoginRateLimitFilter, antes de llegar acá — no en este catch.
            log.warn("Credenciais inválidas para o usuário: {}", email);
            throw new BadCredentialsException("Email ou senha inválidos.");
        }
        AdminUser user =
                adminUserRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos."));
        log.info("Login bem-sucedido — usuário: {}", email);
        return jwtService.generateToken(user);
    }
}
