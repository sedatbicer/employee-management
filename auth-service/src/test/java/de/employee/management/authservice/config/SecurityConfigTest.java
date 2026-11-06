package de.employee.management.authservice.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.employee.management.authservice.auth.controller.AuthController;
import de.employee.management.authservice.auth.dto.LoginRequest;
import de.employee.management.authservice.auth.dto.LoginResponse;
import de.employee.management.authservice.auth.service.AuthService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integrationstests für die Sicherheitskonfiguration des Auth-Service.
 *
 * Überprüft die Zugriffsregeln, die zustandslose Sitzungsverwaltung,
 * die Passwortverschlüsselung und die CORS-Konfiguration.
 *
 * Der MVC-Testkontext verwendet einen simulierten AuthService.
 * Eine echte Datenbankverbindung ist nicht erforderlich.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthService authService;

    /**
     * Prüft, ob der Login-Endpunkt ohne vorherige Authentifizierung
     * aufgerufen werden kann.
     */
    @Test
    void shouldAllowAccessToLoginWithoutAuthentication()
            throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse(
                        "test-jwt-token",
                        "Bearer",
                        3600L,
                        "admin@example.com",
                        "ADMIN"
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@example.com",
                                    "password": "correctPassword"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.email")
                        .value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /**
     * Prüft, ob geschützte Endpunkte ohne Authentifizierung
     * keinen Zugriff erlauben.
     *
     * Da weder HTTP Basic noch Form Login aktiviert sind und
     * kein eigener AuthenticationEntryPoint konfiguriert ist,
     * wird der anonyme Zugriff standardmäßig mit HTTP 403 abgelehnt.
     */
    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/api/auth/protected-test"))
                .andExpect(status().isForbidden());
    }

    /**
     * Prüft, dass die HTTP-Basic-Authentifizierung deaktiviert ist.
     *
     * Auch mit einem Basic-Authorization-Header darf kein Zugriff
     * auf einen geschützten Endpunkt möglich sein.
     */
    @Test
    void shouldNotAllowHttpBasicAuthentication()
            throws Exception {

        mockMvc.perform(get("/api/auth/protected-test")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Basic YWRtaW46cGFzcw=="
                        ))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(
                        HttpHeaders.WWW_AUTHENTICATE
                ));
    }

    /**
     * Prüft, dass die klassische Formularanmeldung deaktiviert ist.
     *
     * Ein nicht authentifizierter Zugriff darf nicht zu einer
     * Weiterleitung auf eine Login-Seite führen.
     */
    @Test
    void shouldNotRedirectToFormLogin()
            throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/auth/protected-test")
                )
                .andExpect(status().isForbidden())
                .andReturn();

        assertNull(
                result.getResponse().getRedirectedUrl(),
                "Es darf keine Weiterleitung auf eine Login-Seite erfolgen."
        );
    }

    /**
     * Prüft, dass bei nicht authentifizierten Anfragen
     * keine serverseitige HTTP-Session angelegt wird.
     */
    @Test
    void shouldUseStatelessSessionManagement()
            throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/api/auth/protected-test")
                )
                .andExpect(status().isForbidden())
                .andReturn();

        assertNull(
                result.getRequest().getSession(false),
                "Es darf keine HTTP-Session angelegt werden."
        );
    }

    /**
     * Prüft, dass der Passwort-Encoder BCrypt verwendet.
     *
     * Das Klartextpasswort darf nicht als Hash gespeichert werden.
     * Das ursprüngliche Passwort muss sich aber korrekt überprüfen lassen.
     */
    @Test
    void shouldEncodeAndVerifyPasswordsWithBCrypt() {

        String rawPassword = "testPassword123!";

        String encodedPassword =
                passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);

        assertTrue(passwordEncoder.matches(
                rawPassword,
                encodedPassword
        ));

        assertTrue(!passwordEncoder.matches(
                "wrongPassword",
                encodedPassword
        ));
    }

    /**
     * Prüft, ob eine gültige CORS-Preflight-Anfrage
     * vom konfigurierten Frontend akzeptiert wird.
     */
    @Test
    void shouldAllowCorsPreflightRequest()
            throws Exception {

        mockMvc.perform(options("/api/auth/login")
                        .header(
                                HttpHeaders.ORIGIN,
                                "http://localhost:4200"
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "POST"
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:4200"
                ))
                .andExpect(header().exists(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS
                ));
    }
}
