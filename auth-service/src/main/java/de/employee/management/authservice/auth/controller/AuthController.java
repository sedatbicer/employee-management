package de.employee.management.authservice.auth.controller;

import de.employee.management.authservice.auth.dto.LoginRequest;
import de.employee.management.authservice.auth.dto.LoginResponse;
import de.employee.management.authservice.auth.service.AuthService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Controller für die Authentifizierung von Benutzern.
 *
 * Stellt den Login-Endpunkt bereit und delegiert die
 * Authentifizierungslogik an den AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authentifiziert einen Benutzer anhand seiner Zugangsdaten.
     *
     * Die Eingabedaten werden vor der Verarbeitung validiert.
     *
     * @param request Login-Daten des Benutzers
     * @return Antwort mit den Authentifizierungsdaten
     */
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}
