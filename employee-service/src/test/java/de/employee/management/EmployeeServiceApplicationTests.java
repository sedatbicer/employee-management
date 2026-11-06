
package de.employee.management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integrationstest für den grundlegenden Start
 * des Employee-Service.
 *
 * @SpringBootTest lädt den vollständigen Spring-
 * Application-Context einschließlich der Konfiguration,
 * Services, Repositories und weiteren Komponenten.
 */
@SpringBootTest
class EmployeeServiceApplicationTests {

    /**
     * Prüft, ob der Spring Application Context
     * ohne Fehler geladen werden kann.
     *
     * Der Test enthält bewusst keine zusätzlichen
     * Assertions. Schlägt das Laden des Contexts fehl,
     * schlägt auch dieser Test automatisch fehl.
     */
    @Test
    void contextLoads() {
    }
}
