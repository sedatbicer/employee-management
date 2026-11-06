package de.employee.management.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit-Tests für den JWT-Authentifizierungsfilter.
 *
 * Prüft die Validierung von JWTs und die Übernahme der
 * Benutzerinformationen in den Spring-Security-Kontext.
 */
class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET =
            "test-secret-key-with-at-least-32-bytes-long";

    private static final String ISSUER = "auth-service";

    private SecretKey secretKey;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private FilterChain filterChain;

    @BeforeEach
    void setUp() {

        secretKey = Keys.hmacShaKeyFor(
                JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );

        filter = new JwtAuthenticationFilter(JWT_SECRET);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        filterChain = (servletRequest, servletResponse) -> {
            // Die weitere Filterkette wird im Unit-Test nicht ausgeführt.
        };

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Prüft, ob ein gültiges JWT den Benutzer authentifiziert.
     */
    @Test
    void shouldAuthenticateUserWithValidToken()
            throws ServletException, IOException {

        String token = createToken(
                "admin@example.com",
                "ADMIN",
                new Date(System.currentTimeMillis() + 3600000),
                ISSUER
        );

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertTrue(authentication.isAuthenticated());

        assertEquals(
                "admin@example.com",
                authentication.getPrincipal()
        );

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                ));

        assertEquals(200, response.getStatus());
    }

    /**
     * Prüft, ob ein JWT mit falscher Signatur abgelehnt wird.
     */
    @Test
    void shouldRejectTokenWithInvalidSignature()
            throws ServletException, IOException {

        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "another-secret-key-with-at-least-32-bytes"
                        .getBytes(StandardCharsets.UTF_8)
        );

        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("admin@example.com")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob ein abgelaufenes JWT abgelehnt wird.
     */
    @Test
    void shouldRejectExpiredToken()
            throws ServletException, IOException {

        String token = createToken(
                "admin@example.com",
                "ADMIN",
                new Date(System.currentTimeMillis() - 60000),
                ISSUER
        );

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob ein JWT mit falschem Aussteller abgelehnt wird.
     */
    @Test
    void shouldRejectTokenWithInvalidIssuer()
            throws ServletException, IOException {

        String token = createToken(
                "admin@example.com",
                "ADMIN",
                new Date(System.currentTimeMillis() + 3600000),
                "unknown-service"
        );

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob ein JWT ohne Benutzeridentität abgelehnt wird.
     */
    @Test
    void shouldRejectTokenWithoutSubject()
            throws ServletException, IOException {

        String token = Jwts.builder()
                .issuer(ISSUER)
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob ein JWT ohne Rollenangabe abgelehnt wird.
     */
    @Test
    void shouldRejectTokenWithoutRole()
            throws ServletException, IOException {

        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("admin@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob eine ungültige Rollenbezeichnung abgelehnt wird.
     */
    @Test
    void shouldRejectTokenWithInvalidRole()
            throws ServletException, IOException {

        String token = createToken(
                "admin@example.com",
                "ROLE_ADMIN",
                new Date(System.currentTimeMillis() + 3600000),
                ISSUER
        );

        addBearerToken(token);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob Anfragen ohne Bearer-Token an die nächste
     * Filterstufe weitergegeben werden.
     */
    @Test
    void shouldContinueFilterChainWithoutBearerToken()
            throws ServletException, IOException {

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Prüft, ob ein zu kurzer JWT-Schlüssel abgelehnt wird.
     */
    @Test
    void shouldRejectSecretShorterThan32Bytes() {

        String shortSecret = "too-short-secret";

        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtAuthenticationFilter(shortSecret)
        );
    }

    /**
     * Erzeugt ein signiertes JWT für die Testfälle.
     */
    private String createToken(
            String subject,
            String role,
            Date expiration,
            String issuer) {

        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Fügt das JWT als Bearer-Token in den HTTP-Header ein.
     */
    private void addBearerToken(String token) {
        request.addHeader("Authorization", "Bearer " + token);
    }
}
