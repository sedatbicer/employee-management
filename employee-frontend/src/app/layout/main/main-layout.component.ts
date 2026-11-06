import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { HeaderComponent } from '../header/header.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

/**
 * Zentrales Anwendungslayout für angemeldete Benutzer.
 *
 * Die Komponente bildet den gemeinsamen visuellen Rahmen
 * der peoplehub-Anwendung.
 *
 * Verantwortlichkeiten:
 * - Einbindung der globalen Kopfzeile (Header).
 * - Einbindung der seitlichen Navigation (Sidebar).
 * - Bereitstellung des Inhaltsbereichs für Feature-Komponenten.
 * - Verwaltung des Öffnungszustands der mobilen Navigation.
 *
 * Öffentliche Seiten, beispielsweise die Login-Seite,
 * werden bewusst außerhalb dieses Layouts dargestellt.
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    HeaderComponent,
    SidebarComponent,
    RouterOutlet
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent {

  /**
   * Steuert den Öffnungszustand der mobilen Sidebar.
   *
   * Die Navigation ist standardmäßig geschlossen,
   * damit auf kleineren Bildschirmen mehr Platz
   * für den eigentlichen Seiteninhalt bleibt.
   */
  readonly mobileNavOpen = signal(false);

  /**
   * Wechselt zwischen geöffnetem und geschlossenem Zustand
   * der mobilen Navigation.
   */
  toggleMobileNavigation(): void {
    this.mobileNavOpen.update(isOpen => !isOpen);
  }

  /**
   * Schließt die mobile Navigation.
   *
   * Wird beispielsweise aufgerufen, wenn ein Benutzer
   * einen Navigationspunkt auswählt oder die Sidebar
   * ausdrücklich geschlossen werden soll.
   */
  closeMobileNavigation(): void {
    this.mobileNavOpen.set(false);
  }

}
