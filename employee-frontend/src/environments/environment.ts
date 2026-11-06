/**
 * Entwicklungskonfiguration der Employee-Management-Anwendung.
 *
 * Diese Einstellungen werden während der lokalen Entwicklung
 * und beim Start des Angular-Entwicklungsservers verwendet.
 *
 * Die API-Endpunkte entsprechen bewusst der Produktionskonfiguration.
 * Dadurch bleibt die Kommunikation zwischen Frontend und Backend
 * unabhängig von der jeweiligen Umgebung einheitlich.
 *
 * Die Weiterleitung der API-Anfragen an das Spring-Boot-Backend
 * erfolgt während der Entwicklung über die konfigurierte
 * Angular-Proxy-Konfiguration.
 */
export const environment = {

  /**
   * Kennzeichnet die lokale Entwicklungsumgebung.
   *
   * Der Wert false signalisiert Angular, dass kein produktiver
   * Build erstellt beziehungsweise ausgeführt wird.
   */
  production: false,

  /**
   * API-Endpunkt für die Mitarbeiterverwaltung.
   *
   * Wird für das Abrufen, Anlegen, Aktualisieren und Löschen
   * von Mitarbeiterdaten verwendet.
   */
  apiEmployee: '/api/employees',

  /**
   * API-Endpunkt für die Authentifizierung.
   *
   * Wird insbesondere für die Anmeldung am Backend verwendet.
   */
  apiAuth: '/api/auth'
};