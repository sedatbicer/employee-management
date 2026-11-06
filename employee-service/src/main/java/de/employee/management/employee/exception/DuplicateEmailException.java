
package de.employee.management.employee.exception;

/**
 * Wird ausgelöst, wenn eine E-Mail-Adresse bereits
 * von einem anderen Mitarbeiter verwendet wird.
 *
 * Dadurch wird eine Verletzung der fachlichen
 * Eindeutigkeitsregel eindeutig gekennzeichnet.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Diese E-Mail-Adresse wird bereits verwendet: " + email);
    }
}
