/**
 * Produktionskonfiguration der Employee-Management-Anwendung.
 *
 * Diese Einstellungen werden beim Erstellen des produktiven
 * Angular-Builds verwendet.
 *
 * Die API-Endpunkte sind bewusst als relative Pfade definiert.
 * Dadurch kann das Frontend die Backend-API unter derselben
 * Domain aufrufen, ohne eine feste Serveradresse zu benötigen.
 *
 * Voraussetzung ist eine entsprechende Bereitstellung beziehungsweise
 * Weiterleitung der API-Anfragen durch den Webserver.
 */
export const environment = {

  /**
   * Kennzeichnet die Produktionsumgebung.
   *
   * Aktiviert die für den produktiven Betrieb vorgesehenen
   * Angular-Build-Einstellungen.
   */
  production: true,

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
