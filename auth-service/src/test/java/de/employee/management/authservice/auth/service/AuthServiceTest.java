
package de.employee.management.authservice.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import de.employee.management.authservice.auth.dto.LoginRequest;
import de.employee.management.authservice.auth.dto.LoginResponse;
import de.employee.management.authservice.auth.entity.AppUser;
import de.employee.management.authservice.auth.repository.AppUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit-Tests für den AuthService.
 *
 * Überprüft die Authentifizierung von Benutzern und das Verhalten
 * bei erfolgreichen sowie fehlgeschlagenen Anmeldeversuchen.
 *
 * Die Abhängigkeiten werden mit Mockito simuliert, sodass keine
 * Datenbankverbindung oder gestartete Spring-Anwendung erforderlich ist.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private AppUser user;

    private LoginRequest loginRequest;

    /**
     * Erstellt vor jedem Test einen Benutzer und gültige Login-Daten.
     */
    @BeforeEach
    void setUp() {

        user = new AppUser();
        user.setEmail("admin@example.com");
        user.setPasswordHash("hashedPassword");
        user.setRole("ADMIN");
        user.setEnabled(true);

        loginRequest = new LoginRequest(
                "admin@example.com",
                "correctPassword"
        );
    }

    /**
     * Prüft eine erfolgreiche Anmeldung mit gültigen Zugangsdaten.
     *
     * Erwartet wird eine LoginResponse mit JWT, Token-Typ,
     * Gültigkeitsdauer in Sekunden und Benutzerinformationen.
     */
    @Test
    void shouldLoginSuccessfully() {

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "correctPassword",
                "hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        when(jwtService.getExpirationMs())
                .thenReturn(3600000L);

        LoginResponse response = authService.login(loginRequest);

        assertEquals("test-jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("admin@example.com", response.email());
        assertEquals("ADMIN", response.role());

        verify(userRepository)
                .findByEmailIgnoreCase("admin@example.com");

        verify(passwordEncoder)
                .matches("correctPassword", "hashedPassword");

        verify(jwtService).generateToken(user);
    }

    /**
     * Prüft, ob Leerzeichen vor und nach der E-Mail-Adresse
     * vor der Benutzersuche entfernt werden.
     */
    @Test
    void shouldTrimEmailBeforeSearchingForUser() {

        LoginRequest request = new LoginRequest(
                "  admin@example.com  ",
                "correctPassword"
        );

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "correctPassword",
                "hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        when(jwtService.getExpirationMs())
                .thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertEquals("test-jwt-token", response.accessToken());

        verify(userRepository)
                .findByEmailIgnoreCase("admin@example.com");
    }

    /**
     * Prüft das Verhalten, wenn kein Benutzer mit der angegebenen
     * E-Mail-Adresse gefunden wird.
     */
    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        // Ohne gefundenen Benutzer dürfen keine weiteren Prüfungen erfolgen.
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    /**
     * Prüft das Verhalten bei einem falschen Passwort.
     */
    @Test
    void shouldThrowExceptionWhenPasswordIsIncorrect() {

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "correctPassword",
                "hashedPassword"
        )).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(passwordEncoder)
                .matches("correctPassword", "hashedPassword");

        // Bei falschem Passwort darf kein JWT erstellt werden.
        verify(jwtService, never()).generateToken(user);
        verify(jwtService, never()).getExpirationMs();
    }

    /**
     * Prüft, dass deaktivierte Benutzer sich nicht anmelden können.
     */
    @Test
    void shouldThrowExceptionWhenUserIsDisabled() {

        user.setEnabled(false);

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        // Deaktivierte Konten werden nicht durch Passwortprüfung freigegeben.
        verify(passwordEncoder, never())
                .matches("correctPassword", "hashedPassword");

        verifyNoInteractions(jwtService);
    }

    /**
     * Prüft, dass bei einer erfolgreichen Anmeldung das Passwort
     * anhand des gespeicherten Passwort-Hashes überprüft wird.
     */
    @Test
    void shouldVerifyPasswordAgainstStoredHash() {

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "correctPassword",
                "hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        when(jwtService.getExpirationMs())
                .thenReturn(3600000L);

        authService.login(loginRequest);

        verify(passwordEncoder)
                .matches("correctPassword", "hashedPassword");
    }

    /**
     * Prüft, dass die Gültigkeitsdauer des JWT in Sekunden
     * und nicht in Millisekunden zurückgegeben wird.
     */
    @Test
    void shouldReturnExpirationInSeconds() {

        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "correctPassword",
                "hashedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        when(jwtService.getExpirationMs())
                .thenReturn(3600000L);

        LoginResponse response = authService.login(loginRequest);

        assertEquals(3600L, response.expiresIn());
    }
}
