
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';

import {
  LoginRequest,
  LoginResponse,
  UserRole
} from '../models/auth.model';

/**
 * Zentraler Service für die Authentifizierung innerhalb der Anwendung.
 *
 * Der Service übernimmt:
 * - Kommunikation mit dem Login-Endpunkt.
 * - Verwaltung des Authentifizierungszustands.
 * - Speicherung der Benutzerdaten im Arbeitsspeicher.
 * - Bereitstellung des Access-Tokens für den HTTP-Interceptor.
 * - Beenden der lokalen Benutzersitzung.
 *
 * Der JWT wird ausschließlich im Arbeitsspeicher gespeichert.
 * Nach einem vollständigen Browser-Reload muss sich der Benutzer
 * erneut anmelden.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiAuth;

  /**
   * Unterstützte Benutzerrollen der Anwendung.
   */
  private readonly validRoles: readonly UserRole[] = [
    'ADMIN',
    'USER'
  ];

  /**
   * Interne Signals für den Authentifizierungszustand.
   *
   * Die Signals sind ausschließlich innerhalb dieses Services
   * veränderbar.
   */
  private readonly tokenSignal = signal<string | null>(null);
  private readonly emailSignal = signal<string | null>(null);
  private readonly roleSignal = signal<UserRole | null>(null);

  /**
   * Gibt an, ob aktuell ein Access-Token vorhanden ist.
   */
  readonly isAuthenticated = computed(
    () => this.tokenSignal() !== null
  );

  /**
   * Öffentlich lesbare E-Mail-Adresse des angemeldeten Benutzers.
   */
  readonly currentEmail = this.emailSignal.asReadonly();

  /**
   * Öffentlich lesbare Rolle des angemeldeten Benutzers.
   */
  readonly currentRole = this.roleSignal.asReadonly();

  /**
   * Authentifiziert einen Benutzer anhand seiner Zugangsdaten.
   *
   * Die Login-Antwort wird vor der Speicherung der Benutzerdaten
   * auf Vollständigkeit und gültige Werte geprüft.
   *
   * Fehler des Backend-Endpunkts oder ungültige Antworten
   * werden an die aufrufende Komponente weitergegeben.
   *
   * @param credentials Zugangsdaten des Benutzers
   * @returns Observable mit der Login-Antwort des Backends
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          this.validateLoginResponse(response);

          // Authentifizierungsdaten erst nach erfolgreicher Prüfung speichern.
          this.tokenSignal.set(response.accessToken);
          this.emailSignal.set(response.email);
          this.roleSignal.set(response.role);
        })
      );
  }

  /**
   * Prüft die vom Backend zurückgegebene Login-Antwort.
   *
   * Es werden nur Antworten akzeptiert, die einen gültigen
   * Access-Token, einen unterstützten Token-Typ, eine positive
   * Gültigkeitsdauer, eine E-Mail-Adresse und eine unterstützte
   * Benutzerrolle enthalten.
   *
   * @param response Antwort des Login-Endpunkts
   * @throws Error bei einer ungültigen Login-Antwort
   */
  private validateLoginResponse(response: LoginResponse): void {

    if (
      !response ||
      typeof response.accessToken !== 'string' ||
      !response.accessToken.trim()
    ) {
      throw new Error(
        'Die Login-Antwort enthält keinen gültigen Access-Token.'
      );
    }

    if (
      typeof response.tokenType !== 'string' ||
      response.tokenType.toLowerCase() !== 'bearer'
    ) {
      throw new Error(
        'Die Login-Antwort enthält einen ungültigen Token-Typ.'
      );
    }

    if (
      typeof response.expiresIn !== 'number' ||
      !Number.isFinite(response.expiresIn) ||
      response.expiresIn <= 0
    ) {
      throw new Error(
        'Die Login-Antwort enthält eine ungültige Token-Gültigkeitsdauer.'
      );
    }

    if (
      typeof response.email !== 'string' ||
      !response.email.trim()
    ) {
      throw new Error(
        'Die Login-Antwort enthält keine gültige E-Mail-Adresse.'
      );
    }

    if (!this.validRoles.includes(response.role)) {
      throw new Error(
        'Die Login-Antwort enthält eine ungültige Benutzerrolle.'
      );
    }
  }

  /**
   * Liefert den aktuell gespeicherten Access-Token.
   *
   * Wird vom HTTP-Interceptor zur Authentifizierung
   * ausgehender API-Anfragen benötigt.
   *
   * @returns Access-Token oder null, wenn kein Token vorhanden ist
   */
  getAccessToken(): string | null {
    return this.tokenSignal();
  }

  /**
   * Beendet die lokale Benutzersitzung.
   *
   * Sämtliche Authentifizierungsinformationen werden
   * aus dem Arbeitsspeicher entfernt.
   *
   * Hinweis:
   * Bei einer zustandslosen JWT-Authentifizierung wird der bereits
   * ausgestellte Token dadurch nicht serverseitig ungültig.
   * Er bleibt bis zum Ablauf verwendbar, sofern das Backend
   * keine zusätzliche serverseitige Sperrung implementiert.
   */
  logout(): void {
    this.tokenSignal.set(null);
    this.emailSignal.set(null);
    this.roleSignal.set(null);
  }
}
