
/**
 * Repräsentiert die im Backend definierten Benutzerrollen.
 *
 * Die explizite Typisierung verhindert ungültige Rollenwerte
 * und ermöglicht eine typsichere Berechtigungsprüfung im Frontend.
 *
 * Die Werte müssen mit den Rollen des Spring-Boot-Backends
 * übereinstimmen.
 */
export type UserRole = 'ADMIN' | 'USER';

/**
 * Enthält die Zugangsdaten, die beim Login an das Backend
 * übermittelt werden.
 *
 * Die Zugangsdaten werden ausschließlich für den
 * Authentifizierungsvorgang verwendet und nicht dauerhaft
 * im AuthService gespeichert.
 */
export interface LoginRequest {

  /** E-Mail-Adresse des Benutzers. */
  email: string;

  /** Passwort des Benutzers. */
  password: string;
}

/**
 * Definiert die erfolgreiche Antwort des Backend-Login-Endpunkts.
 *
 * Das Backend liefert neben dem JWT die wichtigsten Informationen
 * zum angemeldeten Benutzer zurück.
 */
export interface LoginResponse {

  /** JWT zur Authentifizierung nachfolgender API-Anfragen. */
  accessToken: string;

  /** Verwendeter Authentifizierungstyp, üblicherweise "Bearer". */
  tokenType: string;

  /** Gültigkeitsdauer des Access-Tokens in Sekunden. */
  expiresIn: number;

  /** E-Mail-Adresse des erfolgreich angemeldeten Benutzers. */
  email: string;

  /** Rolle des angemeldeten Benutzers. */
  role: UserRole;
}
