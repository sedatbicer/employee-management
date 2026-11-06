import { Routes } from '@angular/router';

// Seitenkomponenten
import { LoginComponent } from './features/auth/login.component';

import {
  EmployeeListComponent
} from './features/employees/employee-list.component';

import {
  EmployeeFormPageComponent
} from './features/employees/employee-form-page.component';

import {
  EmployeeDetailPageComponent
} from './features/employees/employee-detail-page.component';

// Anwendungslayout
import { MainLayoutComponent } from './layout/main/main-layout.component';

// Funktionale Route Guards
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

/**
 * Zentrale Routenkonfiguration der peoplehub-Anwendung.
 *
 * Die Routen sind in öffentliche und geschützte Bereiche unterteilt.
 *
 * Öffentlicher Bereich:
 * - Login-Seite ohne Header und Sidebar.
 *
 * Geschützter Bereich:
 * - Mitarbeiterübersicht
 * - Mitarbeiterdetails
 * - Mitarbeiter anlegen
 * - Mitarbeiter bearbeiten
 *
 * Die geschützten Seiten verwenden das gemeinsame MainLayoutComponent.
 *
 * Die Angular Guards übernehmen die clientseitige Zugriffskontrolle.
 * Die tatsächlichen Berechtigungen werden zusätzlich
 * durch Spring Security im Backend durchgesetzt.
 */
export const routes: Routes = [

  /**
   * Standardroute der Anwendung.
   *
   * Beim Aufruf der Root-Adresse wird der Benutzer
   * zur Login-Seite weitergeleitet.
   */
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },

  /**
   * Öffentlicher Bereich.
   *
   * Die Login-Seite wird bewusst außerhalb des MainLayoutComponent
   * definiert und erhält dadurch keinen Header und keine Sidebar.
   */
  {
    path: 'login',
    component: LoginComponent,
    title: 'peoplehub | Anmelden'
  },

  /**
   * Geschützter Anwendungsbereich.
   *
   * Alle untergeordneten Mitarbeiterseiten werden innerhalb
   * des MainLayoutComponent dargestellt.
   *
   * Dadurch stehen Header und Sidebar zentral zur Verfügung,
   * ohne dass sie in jeder Seitenkomponente eingebunden werden müssen.
   */
  {
    path: '',
    component: MainLayoutComponent,
    children: [

      /**
       * Mitarbeiterübersicht.
       *
       * Zugriff für angemeldete Benutzer mit der Rolle
       * ADMIN oder USER.
       */
      {
        path: 'mitarbeiter',
        component: EmployeeListComponent,
        title: 'peoplehub | Mitarbeiterübersicht',
        canActivate: [authGuard, roleGuard],
        data: {
          roles: ['ADMIN', 'USER']
        }
      },

      /**
       * Mitarbeiter anlegen.
       *
       * Ausschließlich Administratoren dürfen neue Mitarbeiter erstellen.
       */
      {
        path: 'mitarbeiter/neu',
        component: EmployeeFormPageComponent,
        title: 'peoplehub | Mitarbeiter anlegen',
        canActivate: [authGuard, roleGuard],
        data: {
          roles: ['ADMIN']
        }
      },

      /**
       * Mitarbeiter bearbeiten.
       *
       * Ausschließlich Administratoren dürfen bestehende Mitarbeiter ändern.
       */
      {
        path: 'mitarbeiter/:id/bearbeiten',
        component: EmployeeFormPageComponent,
        title: 'peoplehub | Mitarbeiter bearbeiten',
        canActivate: [authGuard, roleGuard],
        data: {
          roles: ['ADMIN']
        }
      },

      /**
       * Mitarbeiterdetails anzeigen.
       *
       * Administratoren und reguläre Benutzer dürfen
       * die Detailinformationen eines Mitarbeiters aufrufen.
       */
      {
        path: 'mitarbeiter/:id',
        component: EmployeeDetailPageComponent,
        title: 'peoplehub | Mitarbeiterdetails',
        canActivate: [authGuard, roleGuard],
        data: {
          roles: ['ADMIN', 'USER']
        }
      }
    ]
  },

  /**
   * Fallback für nicht definierte Routen.
   *
   * Unbekannte Adressen werden zur Login-Seite weitergeleitet.
   */
  {
    path: '**',
    redirectTo: 'login'
  }
];
