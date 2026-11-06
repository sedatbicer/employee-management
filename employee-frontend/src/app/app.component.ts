import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Zentrale Root-Komponente der peoplehub-Anwendung.
 *
 * Die Komponente stellt ausschließlich den RouterOutlet bereit,
 * über den Angular die aktive Route darstellt.
 *
 * Die visuelle Struktur der Anwendung wird durch die jeweiligen
 * Layout-Komponenten verwaltet.
 *
 * Öffentliche Seiten, beispielsweise die Login-Seite,
 * werden ohne zusätzliches Anwendungslayout angezeigt.
 *
 * Geschützte Seiten verwenden das MainLayoutComponent,
 * welches Header, Sidebar und den Inhaltsbereich bereitstellt.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet
  ],
  template: `
    <!-- Stellt die aktuell aktive Route dar. -->
    <router-outlet />
  `
})
export class AppComponent {
}
