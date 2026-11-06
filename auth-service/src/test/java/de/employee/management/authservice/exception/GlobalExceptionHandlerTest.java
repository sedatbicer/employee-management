package de.employee.management.authservice.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.employee.management.authservice.auth.controller.AuthController;
import de.employee.management.authservice.auth.dto.LoginRequest;
import de.employee.management.authservice.auth.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Unit-Tests für die zentrale Fehlerbehandlung des Auth-Service.
 *
 * Überprüft die HTTP-Statuscodes und Fehlerantworten bei fehlgeschlagenen
 * Authentifizierungsversuchen, ungültigen Eingaben und Serverfehlern.
 *
 * Der Controller wird isoliert mit MockMvc getestet. Eine echte
 * Datenbankverbindung oder ein gestarteter Spring-Boot-Server
 * sind dafür nicht erforderlich.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    private AuthService authService;

    private LocalValidatorFactoryBean validator;

    /**
     * Initialisiert den Controller, die zentrale Fehlerbehandlung
     * und die Validierung vor jedem Test.
     */
    @BeforeEach
    void setUp() {

        authService = mock(AuthService.class);

        AuthController authController =
                new AuthController(authService);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * Prüft, ob falsche Login-Daten mit HTTP 401 beantwortet werden.
     *
     * Die Fehlermeldung darf nicht verraten, ob die E-Mail-Adresse
     * unbekannt ist oder das Passwort nicht stimmt.
     */
    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid()
            throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@example.com",
                                    "password": "wrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("E-Mail-Adresse oder Passwort ist ungültig."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Prüft, ob ungültige Login-Daten mit HTTP 400 beantwortet werden.
     *
     * Die Validierungsfehler der einzelnen Felder müssen
     * in der Fehlerantwort enthalten sein.
     */
    @Test
    void shouldReturnBadRequestWhenLoginDataIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "ungueltige-email",
                                    "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Die übermittelten Daten sind ungültig."))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        // Ungültige Eingaben dürfen den Auth-Service nicht erreichen.
        verifyNoInteractions(authService);
    }

    /**
     * Prüft, ob fehlerhaft formatierte JSON-Anfragen
     * mit HTTP 400 beantwortet werden.
     */
    @Test
    void shouldReturnBadRequestWhenRequestBodyIsMalformed()
            throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@example.com",
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Die Anfrage enthält ungültige oder nicht lesbare Daten."));
    }

    /**
     * Prüft, ob unerwartete Fehler mit HTTP 500 beantwortet werden.
     *
     * Interne technische Fehlermeldungen dürfen nicht an den
     * aufrufenden Client weitergegeben werden.
     */
    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs()
            throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(
                        new IllegalStateException(
                                "Interne Datenbankinformationen"
                        )
                );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "admin@example.com",
                                    "password": "correctPassword"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("Ein interner Serverfehler ist aufgetreten."))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "Datenbankinformationen"
                                )
                        )));
    }
}
