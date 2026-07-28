package com.frontpet.identity.api;

import com.frontpet.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ida y vuelta real del login (tarea 1.6): hoy es el único test que pasa por
 * {@code AuthController} con credenciales reales y replica la cookie contra
 * un endpoint protegido. {@link AdminProductControllerTest} finge el usuario
 * autenticado inyectando un {@code AdminUser} en memoria — el JWT nunca se
 * probó de punta a punta hasta este archivo.
 *
 * <p>El usuario usado es el que siembra {@code DataInitializer} al arrancar
 * el contexto (no se persiste uno a mano acá): así el test también ejercita
 * el camino real de seed, y las credenciales se leen de config para no
 * poder desincronizarse de {@code application.yml}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String PROTECTED_URL = "/api/v1/admin/products/images/presign";
    private static final String VALID_PRESIGN_BODY = """
            {"fileName": "foto.png", "contentType": "image/png", "contentLength": 500000}
            """;

    @Value("${frontpet.admin.email}")
    private String adminEmail;

    @Value("${frontpet.admin.password}")
    private String adminPassword;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("login con credenciales válidas devuelve 200 y setea la cookie de sesión")
    void loginWithValidCredentialsSetsCookie() throws Exception {
        MvcResult result = performLogin(adminEmail, adminPassword);

        Cookie cookie = result.getResponse().getCookie("frontpet_session");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(3600);
        // Un JWS tiene 3 segmentos separados por '.': header, payload, firma.
        assertThat(cookie.getValue().split("\\.")).hasSize(3);

        // jakarta.servlet.http.Cookie no tiene accessor de SameSite: hay que
        // leerlo del header Set-Cookie crudo.
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("la cookie de sesión autentica de verdad contra un endpoint protegido")
    void sessionCookieAuthenticatesProtectedEndpoint() throws Exception {
        Cookie sessionCookie = performLogin(adminEmail, adminPassword)
                .getResponse().getCookie("frontpet_session");

        mockMvc.perform(post(PROTECTED_URL)
                        .cookie(sessionCookie)
                        .contentType("application/json")
                        .content(VALID_PRESIGN_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("password incorrecta devuelve 401 sin cookie")
    void loginWithWrongPasswordReturns401WithoutCookie() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content(loginBody(adminEmail, "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha inválidos."))
                .andReturn();

        assertThat(result.getResponse().getCookie("frontpet_session")).isNull();
    }

    @Test
    @DisplayName("email inexistente devuelve el mismo 401 que una password incorrecta (anti-enumeração)")
    void loginWithUnknownEmailReturnsSameBodyAsWrongPassword() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content(loginBody("nao-existe@frontpet.dev", "qualquer-senha")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha inválidos."));
    }

    @Test
    @DisplayName("una cookie con valor inválido no autentica, pero tampoco rompe (401, no 500)")
    void garbageCookieFailsAuthenticationGracefully() throws Exception {
        Cookie garbage = new Cookie("frontpet_session", "isto-nao-e-um-jwt-valido");

        mockMvc.perform(post(PROTECTED_URL)
                        .cookie(garbage)
                        .contentType("application/json")
                        .content(VALID_PRESIGN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout devuelve 200 y expira la cookie de sesión")
    void logoutExpiresSessionCookie() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("Max-Age=0");
    }

    @Test
    @DisplayName("email vacío devuelve 400 con fieldErrors, no llega a autenticar")
    void loginWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content(loginBody("", adminPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    private MvcResult performLogin(String email, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType("application/json")
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
