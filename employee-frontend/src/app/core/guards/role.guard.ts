import { inject } from '@angular/core';

import {
  CanActivateFn,
  Router
} from '@angular/router';

import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.model';

/**
 * Funktionaler Route Guard zur rollenbasierten Zugriffskontrolle.
 *
 * Die für eine Route erforderlichen Rollen werden über
 * das data-Attribut der Angular-Routenkonfiguration definiert.
 *
 * Der Guard prüft, ob die aktuelle Benutzerrolle
 * in der Liste der erlaubten Rollen enthalten ist.
 *
 * Routen ohne explizit definierte Rollenfreigabe
 * werden standardmäßig nicht freigegeben.
 *
 * Die verbindliche Berechtigungsprüfung erfolgt zusätzlich
 * durch Spring Security im Backend.
 */
export const roleGuard: CanActivateFn = (route) => {

  const authService = inject(AuthService);
  const router = inject(Router);

  /**
   * Erforderliche Rollen aus der Routenkonfiguration auslesen.
   *
   * Beispiel:
   * data: { roles: ['ADMIN'] }
   */
  const allowedRoles = route.data['roles'] as UserRole[] | undefined;

  /**
   * Eine Route ohne explizit definierte Rollenfreigabe
   * wird standardmäßig verweigert.
   */
  if (!allowedRoles || allowedRoles.length === 0) {
    return router.createUrlTree(['/mitarbeiter']);
  }

  /**
   * Aktuelle Benutzerrolle aus dem zentralen AuthService beziehen.
   */
  const currentRole = authService.currentRole();

  /**
   * Zugriff nur bei Übereinstimmung mit einer erlaubten Benutzerrolle.
   */
  if (currentRole && allowedRoles.includes(currentRole)) {
    return true;
  }

  /**
   * Benutzer ohne ausreichende Berechtigung
   * zur Mitarbeiterübersicht weiterleiten.
   */
  return router.createUrlTree(['/mitarbeiter']);
};
