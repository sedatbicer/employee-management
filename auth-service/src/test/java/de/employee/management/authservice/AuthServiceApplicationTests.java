package de.employee.management.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integrationstest für den Anwendungskontext des Auth-Service.
 *
 * Überprüft, ob Spring Boot die Anwendung mit allen erforderlichen
 * Komponenten und Konfigurationen erfolgreich starten kann.
 */
@SpringBootTest
class AuthServiceApplicationTests {

    /**
     * Prüft, ob der Spring-Anwendungskontext fehlerfrei geladen wird.
     *
     * Ein erfolgreicher Test zeigt, dass die grundlegende
     * Anwendungskonfiguration funktioniert.
     */
    @Test
    void contextLoads() {
    }
}
