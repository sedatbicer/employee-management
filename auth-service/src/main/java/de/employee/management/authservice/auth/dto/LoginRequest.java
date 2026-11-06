package de.employee.management.authservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO für die Übertragung der Login-Zugangsdaten.
 *
 * Enthält die E-Mail-Adresse und das Passwort eines Benutzers.
 * Die Validierung stellt sicher, dass beide Angaben vorhanden sind
 * und die E-Mail-Adresse ein gültiges Format besitzt.
 *
 * @param email E-Mail-Adresse des Benutzers
 * @param password Passwort des Benutzers
 */
public record LoginRequest(

    @NotBlank(message = "E-Mail-Adresse darf nicht leer sein")
    @Email(message = "Bitte eine gültige E-Mail-Adresse angeben")
    String email,

    @NotBlank(message = "Passwort darf nicht leer sein")
    String password

) {
}
