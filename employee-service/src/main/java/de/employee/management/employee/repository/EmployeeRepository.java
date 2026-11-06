
package de.employee.management.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.employee.management.employee.entity.Employee;

/**
 * Repository für den Datenbankzugriff auf Mitarbeiter.
 *
 * Stellt die grundlegenden CRUD-Operationen für die
 * Employee-Entity bereit.
 *
 * Durch die Erweiterung von JpaRepository übernimmt
 * Spring Data JPA die Implementierung der Standard-
 * Datenbankoperationen automatisch.
 *
 * Die Repository-Schnittstelle enthält keine eigene
 * Implementierung der Datenbankzugriffe.
 */
public interface EmployeeRepository
        extends JpaRepository<Employee, String> {

    /**
     * Prüft, ob bereits ein Mitarbeiter mit der angegebenen
     * E-Mail-Adresse existiert.
     *
     * Der Vergleich erfolgt unabhängig von Groß- und
     * Kleinschreibung.
     *
     * Spring Data JPA leitet die Abfrage automatisch
     * aus dem Methodennamen ab.
     *
     * Die Methode wird im EmployeeService verwendet,
     * um doppelte E-Mail-Adressen vor dem Speichern
     * oder Aktualisieren eines Mitarbeiters zu erkennen.
     *
     * @param email zu prüfende E-Mail-Adresse
     * @return true, wenn die E-Mail-Adresse bereits existiert,
     *         andernfalls false
     */
    boolean existsByEmailIgnoreCase(String email);
}
