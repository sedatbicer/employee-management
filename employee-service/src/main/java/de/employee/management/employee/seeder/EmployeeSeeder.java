
package de.employee.management.employee.seeder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import de.employee.management.employee.entity.Employee;
import de.employee.management.employee.repository.EmployeeRepository;

/**
 * Initialisiert die Mitarbeiterdatenbank mit Beispieldaten.
 *
 * Der Seeder wird beim Start der Spring-Boot-Anwendung
 * automatisch ausgeführt.
 *
 * Er stellt sicher, dass für die Entwicklung und Demo
 * eine definierte Anzahl an Beispieldatensätzen vorhanden ist.
 *
 * Bereits vorhandene Mitarbeiterdaten werden nicht überschrieben.
 */
@Component
public class EmployeeSeeder implements CommandLineRunner {

    private final EmployeeRepository repository;

    /**
     * Injiziert das EmployeeRepository für den Datenbankzugriff.
     *
     * @param repository Repository für Mitarbeiterdaten
     */
    public EmployeeSeeder(EmployeeRepository repository) {
        this.repository = repository;
    }

    /**
     * Wird automatisch nach dem Start des Spring-Boot-Kontexts
     * ausgeführt.
     *
     * Prüft zunächst, ob bereits Mitarbeiterdaten vorhanden sind.
     * Nur bei einer leeren Datenbank werden die Beispieldaten
     * angelegt und gespeichert.
     *
     * @param args Kommandozeilenargumente beim Anwendungsstart
     */
    @Override
    public void run(String... args) {

        // Vorhandene Mitarbeiterdaten nicht überschreiben.
        if (repository.count() > 0) {
            return;
        }

        // Beispieldaten für die Entwicklung und Demo vorbereiten.
        List<Employee> employees = List.of(
            new Employee("PH-1001", "Lena", "Hoffmann",
                "lena.hoffmann@beispiel.de", "Personal",
                "HR Managerin", "Köln",
                LocalDate.parse("2021-03-15"), "Aktiv"),

            new Employee("PH-1002", "Jonas", "Weber",
                "jonas.weber@beispiel.de", "Engineering",
                "Frontend Developer", "Berlin",
                LocalDate.parse("2022-08-01"), "Aktiv"),

            new Employee("PH-1003", "Mia", "Schneider",
                "mia.schneider@beispiel.de", "Design",
                "Product Designerin", "Hamburg",
                LocalDate.parse("2020-11-09"), "Urlaub"),

            new Employee("PH-1004", "Leon", "Fischer",
                "leon.fischer@beispiel.de", "Engineering",
                "Backend Developer", "München",
                LocalDate.parse("2023-01-16"), "Aktiv"),

            new Employee("PH-1005", "Emma", "Schulz",
                "emma.schulz@beispiel.de", "Finanzen",
                "Controllerin", "Köln",
                LocalDate.parse("2019-06-03"), "Aktiv"),

            new Employee("PH-1006", "Noah", "Wagner",
                "noah.wagner@beispiel.de", "Marketing",
                "Marketing Specialist", "Berlin",
                LocalDate.parse("2024-02-12"), "Inaktiv"),

            new Employee("PH-1007", "Sofia", "Becker",
                "sofia.becker@beispiel.de", "Vertrieb",
                "Account Executive", "Frankfurt",
                LocalDate.parse("2022-04-25"), "Aktiv"),

            new Employee("PH-1008", "Paul", "Hoffmann",
                "paul.hoffmann@beispiel.de", "Engineering",
                "QA Engineer", "Hamburg",
                LocalDate.parse("2021-09-01"), "Aktiv"),

            new Employee("PH-1009", "Amira", "Yilmaz",
                "amira.yilmaz@beispiel.de", "Personal",
                "Recruiterin", "Köln",
                LocalDate.parse("2023-07-10"), "Urlaub"),

            new Employee("PH-1010", "Ben", "Krüger",
                "ben.krueger@beispiel.de", "Produkt",
                "Product Manager", "München",
                LocalDate.parse("2020-02-17"), "Aktiv")
        );

        // Alle vorbereiteten Beispieldatensätze gemeinsam speichern.
        repository.saveAll(employees);

        // Bestätigung der erfolgreichen Initialisierung ausgeben.
        System.out.println(
            "People Hub: 10 Seed-Mitarbeiter wurden angelegt."
        );
    }
}
