import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ActivatedRoute,
  Router
} from '@angular/router';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';
import { LoginRequest } from '../../core/models/auth.model';

/**
 * Login-Komponente der peoplehub-Anwendung.
 *
 * Verantwortlichkeiten:
 * - Erfassung und Validierung der Zugangsdaten.
 * - Auslösen des Authentifizierungsvorgangs.
 * - Darstellung des Lade- und Fehlerzustands.
 * - Weiterleitung nach erfolgreicher Anmeldung.
 *
 * Die eigentliche Authentifizierungslogik bleibt
 * im AuthService gekapselt.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);

  /**
   * Formularzustand.
   */
  readonly email = signal('');
  readonly password = signal('');

  /**
   * Steuert die Darstellung des laufenden Login-Vorgangs.
   */
  readonly loading = signal(false);

  /**
   * Enthält eine allgemeine Fehlermeldung.
   */
  readonly errorMessage = signal('');

  /**
   * Gibt an, ob das Formular bereits abgesendet wurde.
   */
  readonly validationAttempted = signal(false);

  /**
   * Validiert die eingegebene E-Mail-Adresse.
   *
   * Die Fehlermeldungen werden erst nach dem ersten
   * Absendeversuch angezeigt.
   */
  readonly emailError = computed(() => {

    if (!this.validationAttempted()) {
      return '';
    }

    const email = this.email().trim();

    if (!email) {
      return 'Bitte gib deine E-Mail-Adresse ein.';
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailPattern.test(email)) {
      return 'Bitte gib eine gültige E-Mail-Adresse ein.';
    }

    return '';
  });

  /**
   * Prüft, ob ein Passwort eingegeben wurde.
   *
   * Das Passwort wird bewusst nicht getrimmt,
   * da Leerzeichen Bestandteil eines gültigen Passworts
   * sein können.
   */
  readonly passwordError = computed(() => {

    if (!this.validationAttempted()) {
      return '';
    }

    if (!this.password()) {
      return 'Bitte gib dein Passwort ein.';
    }

    return '';
  });

  /**
   * Prüft beim Initialisieren, ob bereits eine aktive
   * Benutzersitzung vorhanden ist.
   *
   * Bereits angemeldete Benutzer werden nicht erneut
   * zur Eingabe ihrer Zugangsdaten aufgefordert.
   */
  ngOnInit(): void {

    if (this.authService.isAuthenticated()) {
      this.navigateAfterLogin();
    }
  }

  /**
   * Startet den Login-Vorgang.
   *
   * Vor der Übermittlung werden die Eingaben lokal validiert.
   * Bei erfolgreicher Authentifizierung erfolgt die Weiterleitung
   * zur ursprünglich angeforderten Route oder zur Mitarbeiterübersicht.
   */
  onLogin(): void {

    // Mehrfaches Absenden während einer laufenden Anfrage verhindern.
    if (this.loading()) {
      return;
    }

    this.validationAttempted.set(true);
    this.errorMessage.set('');

    // Formular vor dem Aufruf des Backends vollständig validieren.
    if (this.emailError() || this.passwordError()) {
      return;
    }

    const credentials: LoginRequest = {
      email: this.email().trim(),
      password: this.password()
    };

    this.loading.set(true);

    this.authService
      .login(credentials)
      .pipe(
        // Ladezustand unabhängig vom Ergebnis zurücksetzen.
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: () => {
          this.navigateAfterLogin();
        },

        error: (error: HttpErrorResponse) => {
          this.handleLoginError(error);
        }
      });
  }

  /**
   * Führt die Weiterleitung nach erfolgreicher Anmeldung durch.
   *
   * Vorrangig wird die ursprünglich angeforderte Zieladresse
   * verwendet. Ist diese nicht vorhanden oder ungültig,
   * erfolgt die Weiterleitung zur Mitarbeiterübersicht.
   */
  private navigateAfterLogin(): void {

    const returnUrl = this.getSafeReturnUrl();

    void this.router.navigateByUrl(
      returnUrl ?? '/mitarbeiter'
    );
  }

  /**
   * Behandelt Fehler des Login-Endpunkts.
   *
   * @param error HTTP-Fehler des Backend-Endpunkts
   */
  private handleLoginError(error: HttpErrorResponse): void {

    switch (error.status) {

      case 0:
        this.errorMessage.set(
          'Der Server ist momentan nicht erreichbar. Bitte überprüfe deine Verbindung.'
        );
        break;

      case 401:
        this.errorMessage.set(
          'E-Mail-Adresse oder Passwort ist falsch.'
        );
        break;

      case 403:
        this.errorMessage.set(
          'Die Anmeldung wurde verweigert. Bitte wende dich an den Administrator.'
        );
        break;

      default:
        this.errorMessage.set(
          'Die Anmeldung ist fehlgeschlagen. Bitte versuche es später erneut.'
        );
    }
  }

  /**
   * Ermittelt die ursprünglich angeforderte Zieladresse.
   *
   * Es werden ausschließlich interne Anwendungspfade akzeptiert.
   * Externe Weiterleitungen und die erneute Weiterleitung zur Login-Seite
   * werden verhindert.
   *
   * @returns Sichere interne Zieladresse oder null
   */
  private getSafeReturnUrl(): string | null {

    const returnUrl =
      this.activatedRoute.snapshot.queryParamMap.get('returnUrl');

    if (!returnUrl) {
      return null;
    }

    // Nur interne absolute Pfade akzeptieren.
    if (
      !returnUrl.startsWith('/') ||
      returnUrl.startsWith('//') ||
      returnUrl.includes('\\')
    ) {
      return null;
    }

    // Die Login-Seite selbst ist kein sinnvolles Rückleitungsziel.
    if (
      returnUrl === '/login' ||
      returnUrl.startsWith('/login?') ||
      returnUrl.startsWith('/login#')
    ) {
      return null;
    }

    return returnUrl;
  }
}
