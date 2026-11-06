
package de.employee.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Employee-Service-Anwendung.
 *
 * Die Annotation @SpringBootApplication aktiviert die
 * Spring-Boot-Autokonfiguration, das Component-Scanning
 * und die Spring-Konfiguration.
 *
 * Dadurch erkennt Spring automatisch die Komponenten
 * innerhalb dieses Package-Bereichs und seiner Unterpakete.
 */
@SpringBootApplication
public class EmployeeServiceApplication {

    /**
     * Startet die Spring-Boot-Anwendung.
     *
     * @param args Kommandozeilenargumente beim Anwendungsstart
     */
    public static void main(String[] args) {

        // Spring-Anwendung initialisieren und starten.
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
