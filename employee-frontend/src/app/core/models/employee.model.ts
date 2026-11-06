/**
 * Mögliche Beschäftigungsstatus eines Mitarbeiters.
 *
 * Der Status wird zur Darstellung und Verwaltung
 * des aktuellen Mitarbeiterzustands verwendet.
 */
export type EmployeeStatus = 'Aktiv' | 'Urlaub' | 'Inaktiv';

/**
 * Datenmodell eines Mitarbeiters.
 *
 * Beschreibt die Mitarbeiterdaten, die vom Backend
 * geladen und in der Anwendung dargestellt werden.
 */
export interface Employee {

  // Eindeutige ID des Mitarbeiters aus der Datenbank
  id: string;

  // Vor- und Nachname des Mitarbeiters
  firstName: string;
  lastName: string;

  // E-Mail-Adresse für die Kontaktaufnahme
  email: string;

  // Organisatorische Zuordnung des Mitarbeiters
  department: string;

  // Funktion oder Position innerhalb des Unternehmens
  role: string;

  // Standort des Mitarbeiters
  location: string;

  // Eintrittsdatum des Mitarbeiters (z. B. YYYY-MM-DD)
  startDate: string;

  // Aktueller Beschäftigungsstatus
  status: EmployeeStatus;
}

/**
 * Eingabemodell zum Anlegen und Aktualisieren eines Mitarbeiters.
 *
 * Die ID wird nicht benötigt, da sie vom Backend
 * beim Anlegen automatisch vergeben wird.
 */
export type EmployeeInput = Omit<Employee, 'id'>;
