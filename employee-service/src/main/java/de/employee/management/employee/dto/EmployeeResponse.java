
package de.employee.management.employee.dto;

import java.time.LocalDate;

/**
 * DTO zur Rückgabe von Mitarbeiterdaten über die REST-API.
 *
 * Die Klasse trennt das externe API-Datenmodell von der
 * internen JPA-Entity. Dadurch können Änderungen an der
 * Datenbankstruktur unabhängig vom API-Vertrag erfolgen.
 *
 * Die Mitarbeiter-ID wird vom Backend verwaltet und
 * gehört deshalb nicht zum EmployeeRequest.
 */
public record EmployeeResponse(

        String id,
        String firstName,
        String lastName,
        String email,
        String department,
        String role,
        String location,
        LocalDate startDate,
        String status

) {
}
