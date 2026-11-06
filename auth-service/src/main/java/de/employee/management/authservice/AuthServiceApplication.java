package de.employee.management.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startklasse des Authentifizierungsdienstes.
 *
 * Startet die Spring-Boot-Anwendung und aktiviert die automatische
 * Konfiguration sowie die Suche nach Spring-Komponenten.
 *
 * Da sich diese Klasse im übergeordneten Anwendungspaket befindet,
 * werden auch alle zugehörigen Unterpakete automatisch berücksichtigt.
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * Startet den Authentifizierungsdienst.
     *
     * @param args Kommandozeilenargumente beim Anwendungsstart
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
