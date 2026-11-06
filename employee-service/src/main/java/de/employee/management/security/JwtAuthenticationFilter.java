
package de.employee.management.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter zur Authentifizierung eingehender HTTP-Anfragen anhand eines JWT.
 *
 * Überprüft die Signatur, den Aussteller und die Gültigkeit des Tokens.
 * Bei erfolgreicher Prüfung werden Benutzeridentität und Rolle in den
 * Spring-Security-Kontext übernommen.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String EXPECTED_ISSUER = "auth-service";

    private final SecretKey secretKey;

    /**
     * Erstellt den kryptografischen Schlüssel zur Überprüfung
     * der JWT-Signaturen.
     *
     * @param secret gemeinsames JWT-Geheimnis
     */
    public JwtAuthenticationFilter(
            @Value("${security.jwt.secret}") String secret) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes long"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Prüft einen gegebenenfalls übermittelten JWT und authentifiziert
     * den Benutzer bei erfolgreicher Validierung.
     *
     * @param request eingehende HTTP-Anfrage
     * @param response ausgehende HTTP-Antwort
     * @param filterChain weitere Filter der Sicherheitskette
     * @throws ServletException bei Fehlern innerhalb der Filterkette
     * @throws IOException bei Ein- oder Ausgabefehlern
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Ohne Bearer-Token übernimmt die nachgelagerte Sicherheitskonfiguration
        // die Entscheidung, ob der Zugriff erlaubt ist.
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // Signatur, Ablaufdatum und Aussteller des Tokens überprüfen.
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(EXPECTED_ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // Nur vollständige und syntaktisch gültige Benutzerinformationen
            // dürfen für die Authentifizierung verwendet werden.
            if (email == null || email.isBlank()
                    || role == null || role.isBlank()) {

                rejectRequest(response);
                return;
            }

            String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

            // Verhindert ungültige Rollenbezeichnungen und doppelte ROLE-Präfixe.
            if (!normalizedRole.matches("[A-Z][A-Z0-9_]{0,63}")
                    || normalizedRole.startsWith("ROLE_")) {

                rejectRequest(response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                var authorities = List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + normalizedRole
                        )
                );

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                email.trim(),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (io.jsonwebtoken.JwtException
                | IllegalArgumentException ex) {

            rejectRequest(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Lehnt eine Anfrage mit einem ungültigen JWT ab.
     *
     * @param response HTTP-Antwort
     * @throws IOException bei Fehlern beim Schreiben der Antwort
     */
    private void rejectRequest(HttpServletResponse response)
            throws IOException {

        SecurityContextHolder.clearContext();

        response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Invalid or expired token"
        );
    }
}
