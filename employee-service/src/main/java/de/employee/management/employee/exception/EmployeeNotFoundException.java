
package de.employee.management.employee.exception;

/**
 * Wird ausgelöst, wenn ein angeforderter Mitarbeiter
 * anhand seiner ID nicht gefunden werden kann.
 *
 * Die Exception beschreibt einen fachlichen Fehler
 * unabhängig von der späteren HTTP-Fehlerbehandlung.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String id) {
        super("Mitarbeiter nicht gefunden: " + id);
    }
}
