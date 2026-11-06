package de.employee.management.authservice.auth.dto;

/**
 * DTO für die Rückgabe erfolgreicher Authentifizierungsdaten.
 *
 * Übermittelt das JWT und zusätzliche Informationen,
 * die der Client für die weitere Authentifizierung benötigt.
 *
 * @param accessToken JWT für authentifizierte API-Anfragen
 * @param tokenType Typ des Tokens, üblicherweise Bearer
 * @param expiresIn Gültigkeitsdauer des Tokens in Sekunden
 * @param email E-Mail-Adresse des authentifizierten Benutzers
 * @param role Rolle des authentifizierten Benutzers
 */
public record LoginResponse(

    String accessToken,
    String tokenType,
    long expiresIn,
    String email,
    String role

) {
}
