import {
  Component,
  inject,
  output,
  signal
} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

/**
 * Stellt die globale Kopfzeile des authentifizierten Anwendungsbereichs dar.
 *
 * Verantwortlichkeiten:
 * - Anzeige der aktuellen Seitenbezeichnung anhand der Router-URL.
 * - Darstellung des aktuellen Datums im deutschen Langformat.
 * - Bereitstellung der mobilen Navigationssteuerung.
 *
 * Die Benutzerinformationen und die Abmeldefunktion werden ausschließlich
 * in der Sidebar verwaltet, um doppelte Darstellungen zu vermeiden.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {

  private readonly router = inject(Router);

  /**
   * Signal-Output zum Öffnen und Schließen der mobilen Navigation.
   *
   * Das übergeordnete Layout empfängt dieses Ereignis und steuert
   * darüber den geöffneten Zustand der Sidebar.
   */
  readonly menuToggle = output<void>();

  /**
   * Aktuelles Datum im deutschen Langformat.
   *
   * Die Formatierung wird bei der Initialisierung der Komponente
   * einmal ausgeführt.
   */
  readonly today = new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'full'
  }).format(new Date());

  /**
   * Aktuelle Seitenbezeichnung für die Breadcrumb-Anzeige.
   *
   * Der Initialwert wird anhand der beim Erzeugen der Komponente
   * bereits aktiven Router-URL ermittelt.
   */
  readonly pageTitle = signal(
    this.resolvePageTitle(this.router.url)
  );

  constructor() {
    /**
     * Beobachtet erfolgreiche Router-Navigationen und aktualisiert
     * die Breadcrumb-Anzeige anhand der neuen URL.
     *
     * NavigationEnd stellt sicher, dass nur vollständig abgeschlossene
     * Navigationen berücksichtigt werden.
     *
     * takeUntilDestroyed beendet das Abonnement automatisch,
     * sobald die Komponente zerstört wird.
     */
    this.router.events
      .pipe(
        filter(
          (event): event is NavigationEnd =>
            event instanceof NavigationEnd
        ),
        takeUntilDestroyed()
      )
      .subscribe(event => {
        this.pageTitle.set(
          this.resolvePageTitle(event.urlAfterRedirects)
        );
      });
  }

  /**
   * Ermittelt anhand der aktuellen Router-URL die passende Seitenbezeichnung.
   *
   * Die Methode berücksichtigt die vorhandenen Mitarbeiter-Routen
   * und ordnet ihnen eine verständliche deutsche Bezeichnung zu.
   *
   * @param url Aktuelle Router-URL einschließlich möglicher Parameter.
   * @returns Lesbare Bezeichnung der aktuell aufgerufenen Seite.
   */
  private resolvePageTitle(url: string): string {
    const path = url.split('?')[0].split('#')[0];
    const segments = path.split('/').filter(Boolean);

    if (segments[0] !== 'mitarbeiter') {
      return 'Übersicht';
    }

    if (segments.length === 1) {
      return 'Mitarbeiter';
    }

    if (segments[1] === 'neu') {
      return 'Mitarbeiter anlegen';
    }

    if (segments[2] === 'bearbeiten') {
      return 'Mitarbeiter bearbeiten';
    }

    return 'Mitarbeiterdetails';
  }
}
