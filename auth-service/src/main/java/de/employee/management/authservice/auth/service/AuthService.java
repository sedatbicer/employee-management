package de.employee.management.authservice.auth.service;

import de.employee.management.authservice.auth.dto.LoginRequest;
import de.employee.management.authservice.auth.dto.LoginResponse;
import de.employee.management.authservice.auth.entity.AppUser;
import de.employee.management.authservice.auth.repository.AppUserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Fachlicher Service für die Authentifizierung von Benutzern.
 *
 * Verantwortlich für die Prüfung der Zugangsdaten und die
 * Erstellung der Authentifizierungsantwort mit JWT.
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authentifiziert einen Benutzer anhand seiner Zugangsdaten.
     *
     * Bei ungültigen Zugangsdaten oder deaktiviertem Benutzerkonto
     * wird bewusst dieselbe Fehlermeldung zurückgegeben.
     *
     * @param request übermittelte Login-Daten
     * @return Authentifizierungsantwort mit JWT und Benutzerinformationen
     * @throws BadCredentialsException bei ungültigen Zugangsdaten
     *                                  oder deaktiviertem Benutzerkonto
     */
    public LoginResponse login(LoginRequest request) {

        AppUser user = userRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!user.isEnabled()
                || !passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash())) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Das JWT wird erst nach erfolgreicher Authentifizierung erstellt.
        String token = jwtService.generateToken(user);

        // Die Gültigkeitsdauer wird gemäß API-Konvention in Sekunden angegeben.
        long expiresInSeconds = jwtService.getExpirationMs() / 1000;

        return new LoginResponse(
                token,
                "Bearer",
                expiresInSeconds,
                user.getEmail(),
                user.getRole()
        );
    }
}
