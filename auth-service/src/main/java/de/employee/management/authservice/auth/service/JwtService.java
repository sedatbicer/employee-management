package de.employee.management.authservice.auth.service;

import de.employee.management.authservice.auth.entity.AppUser;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service für die Erstellung signierter JSON Web Tokens (JWT).
 *
 * Erstellt zeitlich begrenzte Zugriffstoken mit Benutzeridentität
 * und Rolleninformationen für die Authentifizierung an den APIs.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs) {

        // Für HMAC-SHA256 werden mindestens 256 Bit Schlüsselmaterial benötigt.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "Das JWT-Secret muss mindestens 32 Bytes lang sein."
            );
        }

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "Die JWT-Gültigkeitsdauer muss größer als null sein."
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Erstellt ein signiertes JWT für einen authentifizierten Benutzer.
     *
     * Das Token enthält die E-Mail-Adresse als Subject sowie die Rolle
     * des Benutzers und ein zeitlich begrenztes Ablaufdatum.
     *
     * @param user authentifizierter Benutzer
     * @return signiertes JWT
     */
    public String generateToken(AppUser user) {

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
                .issuer("auth-service")
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Liefert die konfigurierte Gültigkeitsdauer des JWT.
     *
     * @return Gültigkeitsdauer in Millisekunden
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
