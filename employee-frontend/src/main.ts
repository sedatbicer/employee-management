
import { bootstrapApplication } from '@angular/platform-browser';

import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

/**
 * Startet die Angular-Anwendung.
 *
 * Die AppComponent dient als zentrale Root-Komponente.
 * Die appConfig stellt die globalen Provider bereit,
 * beispielsweise Routing und HTTP-Interceptor.
 *
 * Fehler beim Starten der Anwendung werden in der Konsole ausgegeben.
 */
bootstrapApplication(AppComponent, appConfig).catch(
  (error: unknown) => {
    console.error('Fehler beim Starten der Anwendung:', error);
  }
);
