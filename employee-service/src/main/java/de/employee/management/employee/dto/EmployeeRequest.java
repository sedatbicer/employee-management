package de.employee.management.employee.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO für eingehende Mitarbeiterdaten über die REST-API.
 *
 * Das Request-DTO definiert, welche Daten beim Anlegen
 * oder Aktualisieren eines Mitarbeiters übermittelt werden.
 *
 * Durch die Trennung von Request-DTO und JPA-Entity wird
 * verhindert, dass externe API-Anfragen direkt an das
 * interne Datenbankmodell gebunden sind.
 *
 * Die Validierungsannotationen stellen sicher, dass
 * erforderliche Eingabedaten bereits vor der Verarbeitung
 * durch den Service geprüft werden.
 *
 * Die Mitarbeiter-ID ist bewusst nicht enthalten, da sie
 * ausschließlich vom Backend verwaltet wird.
 *
 * @param firstName Vorname des Mitarbeiters
 * @param lastName Nachname des Mitarbeiters
 * @param email E-Mail-Adresse des Mitarbeiters
 * @param department Abteilung des Mitarbeiters
 * @param role Funktion oder Position des Mitarbeiters
 * @param location Arbeitsort des Mitarbeiters
 * @param startDate Eintrittsdatum des Mitarbeiters
 * @param status aktueller Beschäftigungsstatus
 */
public record EmployeeRequest(

        /**
         * Vorname des Mitarbeiters.
         * Darf weder null noch leer oder ausschließlich aus
         * Leerzeichen bestehen.
         */
        @NotBlank(message = "Vorname ist erforderlich")
        String firstName,

        /**
         * Nachname des Mitarbeiters.
         * Darf weder null noch leer oder ausschließlich aus
         * Leerzeichen bestehen.
         */
        @NotBlank(message = "Nachname ist erforderlich")
        String lastName,

        /**
         * E-Mail-Adresse des Mitarbeiters.
         * Muss angegeben werden und ein gültiges E-Mail-Format haben.
         */
        @NotBlank(message = "E-Mail ist erforderlich")
        @Email(message = "Ungültige E-Mail-Adresse")
        String email,

        /**
         * Abteilung, der der Mitarbeiter zugeordnet ist.
         */
        String department,

        /**
         * Funktion oder berufliche Position des Mitarbeiters.
         */
        String role,

        /**
         * Standort oder Arbeitsort des Mitarbeiters.
         */
        String location,

        /**
         * Datum des Beschäftigungsbeginns.
         * Muss angegeben werden.
         */
        @NotNull(message = "Eintrittsdatum ist erforderlich")
        LocalDate startDate,

        /**
         * Aktueller Beschäftigungsstatus des Mitarbeiters.
         * Darf nicht leer sein.
         */
        @NotBlank(message = "Status ist erforderlich")
        String status
) {
}
