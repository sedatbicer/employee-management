
import {
  Component,
  computed,
  inject,
  input,
  output
} from '@angular/core';

import {
  Router,
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import { EmployeeService } from '../../core/services/employee.service';
import { AuthService } from '../../core/services/auth.service';

/**
 * Seitliche Hauptnavigation der peoplehub-Anwendung.
 *
 * Verantwortlichkeiten:
 * - Darstellung der verfügbaren Navigationsbereiche.
 * - Anzeige der aktuellen Mitarbeiteranzahl.
 * - Anzeige der angemeldeten Benutzerinformationen.
 * - Steuerung des mobilen Öffnungszustands.
 * - Abmeldung des aktuell angemeldeten Benutzers.
 *
 * Die Komponente verwendet die bestehenden Services,
 * um Benutzer- und Mitarbeiterinformationen abzurufen.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  /**
   * Zugriff auf die Mitarbeiterdaten.
   *
   * Die Mitarbeiteranzahl wird direkt aus dem Service
   * übernommen und bei Änderungen automatisch aktualisiert.
   */
  readonly employeeService = inject(EmployeeService);

  /**
   * Steuert die Sichtbarkeit der Sidebar auf mobilen Geräten.
   *
   * Der Wert wird von der übergeordneten Layout-Komponente
   * übergeben.
   */
  readonly open = input(false);

  /**
   * Signalisiert der übergeordneten Layout-Komponente,
   * dass die mobile Navigation geschlossen werden soll.
   */
  readonly close = output<void>();

  /**
   * E-Mail-Adresse des aktuell angemeldeten Benutzers.
   */
  readonly userEmail = this.authService.currentEmail;

  /**
   * Rolle des aktuell angemeldeten Benutzers.
   */
  readonly userRole = this.authService.currentRole;

  /**
   * Ermittelt die passende Bezeichnung für die Benutzerrolle.
   */
  readonly roleLabel = computed(() => {
    switch (this.userRole()) {
      case 'ADMIN':
        return 'Administrator';

      case 'USER':
        return 'Mitarbeiter';

      default:
        return 'Benutzer';
    }
  });

  /**
   * Erzeugt Initialen aus der E-Mail-Adresse des Benutzers.
   *
   * Bei einer E-Mail-Adresse mit mehreren Namensbestandteilen
   * werden die Anfangsbuchstaben der ersten beiden Bestandteile
   * verwendet.
   *
   * Beispiel:
   * max.mustermann@peoplehub.de -> MM
   *
   * Bei einem einzelnen Bestandteil werden die ersten beiden
   * Zeichen verwendet.
   */
  readonly userInitials = computed(() => {
    const email = this.userEmail();

    if (!email) {
      return '??';
    }

    const localPart = email.split('@')[0];

    const nameParts = localPart
      .split(/[._-]+/)
      .filter(Boolean);

    if (nameParts.length >= 2) {
      return nameParts
        .slice(0, 2)
        .map(part => part.charAt(0))
        .join('')
        .toUpperCase();
    }

    return localPart
      .slice(0, 2)
      .toUpperCase();
  });

  /**
   * Meldet den aktuell angemeldeten Benutzer ab.
   *
   * Der AuthService entfernt dabei die gespeicherten
   * Authentifizierungsinformationen.
   *
   * Anschließend wird der Benutzer zur Login-Seite
   * weitergeleitet.
   */
  logout(): void {
    this.authService.logout();
    this.close.emit();

    void this.router.navigateByUrl('/login');
  }

}
