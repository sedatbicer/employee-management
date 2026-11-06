import { inject } from '@angular/core';

import {
  CanActivateFn,
  Router
} from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Funktionaler Route Guard zur Prüfung des Authentifizierungsstatus.
 *
 * Der Guard verhindert den Zugriff auf geschützte Routen,
 * solange keine aktive lokale Authentifizierung vorliegt.
 *
 * Nicht angemeldete Benutzer werden zur Login-Seite weitergeleitet.
 * Die ursprünglich angeforderte URL wird als returnUrl übergeben,
 * damit nach erfolgreicher Anmeldung eine gezielte Rückleitung
 * erfolgen kann.
 *
 * Die verbindliche Zugriffskontrolle erfolgt zusätzlich
 * durch Spring Security im Backend.
 */
export const authGuard: CanActivateFn = (_route, state) => {

  const authService = inject(AuthService);
  const router = inject(Router);

  /**
   * Prüfen, ob eine aktive Authentifizierung vorliegt.
   */
  if (authService.isAuthenticated()) {
    return true;
  }

  /**
   * Nicht angemeldete Benutzer zur Login-Seite weiterleiten.
   *
   * Die ursprünglich angeforderte URL wird als Query-Parameter
   * übergeben, damit nach erfolgreicher Anmeldung eine Rückleitung
   * zur gewünschten Seite möglich ist.
   */
  return router.createUrlTree(
    ['/login'],
    {
      queryParams: {
        returnUrl: state.url
      }
    }
  );
};
