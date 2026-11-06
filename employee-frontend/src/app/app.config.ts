
import { ApplicationConfig } from '@angular/core';
import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

/**
 * Zentrale Konfiguration der Angular-Anwendung.
 *
 * Hier werden die global benötigten Provider registriert,
 * die anschließend in der gesamten Anwendung verfügbar sind.
 *
 * Dazu gehören:
 * - Angular Router für die Navigation.
 * - HttpClient für die Kommunikation mit dem Backend.
 * - AuthInterceptor für die automatische JWT-Übertragung.
 */
export const appConfig: ApplicationConfig = {
  providers: [

    /**
     * Registriert das Angular-Routing und stellt
     * die definierten Anwendungsrouten bereit.
     */
    provideRouter(routes),

    /**
     * Registriert den HttpClient für HTTP-Anfragen.
     *
     * Der AuthInterceptor ergänzt ausschließlich
     * freigegebene Backend-Anfragen um den JWT-Token.
     */
    provideHttpClient(
      withInterceptors([
        authInterceptor
      ])
    )
  ]
};
