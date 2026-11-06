import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Employee } from '../../core/models/employee.model';
import { EmployeeService } from '../../core/services/employee.service';

/**
 * Detailseite zur Anzeige eines einzelnen Mitarbeiters.
 *
 * Die Komponente stellt die vollständigen Stammdaten eines Mitarbeiters dar
 * und ermöglicht den Zugriff auf die Bearbeitungs- und Löschfunktionen.
 *
 * Die Mitarbeiterdaten werden anhand der ID aus der aktuellen Route
 * über den EmployeeService vom Backend geladen.
 */
@Component({
  selector: 'app-employee-detail-page',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],

  template: `
    <section class="page-content detail-page">

      <!-- Ladeanzeige während der Backend-Anfrage. -->
      @if (loading()) {
        <article class="detail-card">
          <h2>Mitarbeiterdaten werden geladen ...</h2>
        </article>
      } @else if (employee(); as person) {

        <!-- Kopfbereich mit Mitarbeiterinformationen und Aktionen. -->
        <div class="page-heading">
          <div>
            <div class="eyebrow">MITARBEITERPROFIL</div>
            <h1>{{ person.firstName }} {{ person.lastName }}</h1>
            <p>
              Personalnummer {{ person.id }} · {{ person.department }}
            </p>
          </div>

          <!-- Aktionen zur Bearbeitung und Löschung des Mitarbeiters. -->
          <div class="actions">
            <a
              class="primary-button"
              [routerLink]="['/mitarbeiter', person.id, 'bearbeiten']">
              Bearbeiten
            </a>

            <button
              class="delete-button"
              type="button"
              [disabled]="loading()"
              (click)="deleteEmployee(person.id, person.firstName, person.lastName)">
              Löschen
            </button>
          </div>
        </div>

        <!-- Darstellung der persönlichen und beruflichen Mitarbeiterdaten. -->
        <article class="detail-card">
          <h2>Persönliche Angaben</h2>

          <dl>
            <dt>Vorname</dt>
            <dd>{{ person.firstName }}</dd>

            <dt>Nachname</dt>
            <dd>{{ person.lastName }}</dd>

            <dt>E-Mail-Adresse</dt>
            <dd>
              <a [href]="'mailto:' + person.email">
                {{ person.email }}
              </a>
            </dd>

            <dt>Abteilung</dt>
            <dd>{{ person.department }}</dd>

            <dt>Position</dt>
            <dd>{{ person.role }}</dd>

            <dt>Standort</dt>
            <dd>{{ person.location }}</dd>

            <dt>Eintrittsdatum</dt>
            <dd>{{ person.startDate | date:'dd.MM.yyyy' }}</dd>

            <dt>Status</dt>
            <dd>
              <span
                class="status-pill"
                [class.status-aktiv]="person.status === 'Aktiv'">
                {{ person.status }}
              </span>
            </dd>
          </dl>
        </article>

      } @else {

        <!-- Fehlermeldung, wenn kein Mitarbeiterdatensatz gefunden wurde. -->
        <article class="detail-card">
          <h2>Mitarbeiter nicht gefunden</h2>
          <p>{{ errorMessage() || 'Dieser Datensatz ist nicht vorhanden.' }}</p>
        </article>
      }

      <!-- Navigation zurück zur Mitarbeiterübersicht. -->
      <p class="back-link">
        <a routerLink="/mitarbeiter">← Zurück zur Übersicht</a>
      </p>

    </section>
  `,

  styles: [`
    .detail-page {
      max-width:1050px;
    }

    .actions {
      display:flex;
      gap:10px;
      align-items:center;
    }

    .detail-card {
      padding:28px;
      border:1px solid #e9edf4;
      border-radius:14px;
      background:#fff;
    }

    .detail-card h2 {
      margin:0 0 24px;
      font-size:18px;
    }

    dl {
      display:grid;
      grid-template-columns:200px minmax(0,1fr);
      gap:18px 14px;
      margin:0;
    }

    dt {
      color:#7b879b;
    }

    dd {
      margin:0;
      color:#34415a;
      font-weight:600;
    }

    .back-link {
      margin-top:22px;
    }

    .back-link a {
      color:#3159c9;
      text-decoration:none;
    }

    .primary-button {
      display:inline-flex;
      align-items:center;
      text-decoration:none;
    }

    @media(max-width:600px) {
      dl {
        grid-template-columns:1fr;
        gap:6px;
      }

      dd {
        margin-bottom:12px;
      }

      .page-heading {
        align-items:flex-start;
        flex-direction:column;
      }
    }
  `]
})
export class EmployeeDetailPageComponent implements OnInit {

  /** Angular-Dienste für Routenverwaltung, Navigation und Datenzugriff. */
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);

  /**
   * Mitarbeiter-ID aus den aktuellen Routenparametern.
   *
   * Die ID wird benötigt, um den entsprechenden Datensatz
   * vom Backend abzurufen und gegebenenfalls zu löschen.
   */
  private readonly id = this.route.snapshot.paramMap.get('id');

  /** Enthält die aktuell geladenen Mitarbeiterdaten. */
  readonly employee = signal<Employee | null>(null);

  /** Steuert den Ladezustand während der Backend-Kommunikation. */
  readonly loading = signal(false);

  /** Enthält eine allgemeine Fehlermeldung für den Benutzer. */
  readonly errorMessage = signal('');

  /**
   * Initialisiert die Detailseite.
   *
   * Zunächst wird geprüft, ob eine Mitarbeiter-ID in der Route vorhanden ist.
   * Anschließend werden die zugehörigen Mitarbeiterdaten vom Backend geladen.
   */
  ngOnInit(): void {
    if (!this.id) {
      this.errorMessage.set('Keine Mitarbeiter-ID angegeben.');
      return;
    }

    this.loadEmployee(this.id);
  }

  /**
   * Lädt einen Mitarbeiter anhand seiner ID vom Backend.
   *
   * Bei erfolgreicher Anfrage werden die empfangenen Daten
   * im employee-Signal gespeichert.
   *
   * Schlägt die Anfrage fehl, wird der Mitarbeiterzustand zurückgesetzt
   * und eine entsprechende Fehlermeldung angezeigt.
   *
   * @param id Eindeutige ID des Mitarbeiters
   */
  private loadEmployee(id: string): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.employeeService.findById(id).subscribe({
      next: employee => {
        // Geladene Mitarbeiterdaten für das Template bereitstellen.
        this.employee.set(employee);
        this.loading.set(false);
      },

      error: error => {
        // Technische Fehler für die Entwicklung protokollieren.
        console.error(
          'Mitarbeiter konnte nicht geladen werden:',
          error
        );

        this.employee.set(null);
        this.loading.set(false);
        this.errorMessage.set(
          'Mitarbeiter konnte nicht geladen werden.'
        );
      }
    });
  }

  /**
   * Löscht einen Mitarbeiter nach vorheriger Benutzerbestätigung.
   *
   * Vor dem Löschen wird eine Sicherheitsabfrage angezeigt.
   * Erst nach ausdrücklicher Bestätigung wird die Löschanfrage
   * an das Backend übermittelt.
   *
   * Bei erfolgreicher Löschung erfolgt die Weiterleitung
   * zur Mitarbeiterübersicht.
   *
   * @param id Eindeutige ID des Mitarbeiters
   * @param firstName Vorname des Mitarbeiters
   * @param lastName Nachname des Mitarbeiters
   */
  deleteEmployee(
    id: string,
    firstName: string,
    lastName: string
  ): void {

    // Löschvorgang erst nach ausdrücklicher Benutzerbestätigung starten.
    if (!window.confirm(
      `Soll ${firstName} ${lastName} wirklich gelöscht werden?`
    )) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.employeeService.delete(id).subscribe({
      next: () => {
        this.loading.set(false);

        // Nach erfolgreicher Löschung zur Mitarbeiterübersicht wechseln.
        void this.router.navigate(['/mitarbeiter']);
      },

      error: error => {
        // Technische Fehler für die Entwicklung protokollieren.
        console.error(
          'Mitarbeiter konnte nicht gelöscht werden:',
          error
        );

        this.loading.set(false);
        this.errorMessage.set(
          'Mitarbeiter konnte nicht gelöscht werden.'
        );
      }
    });
  }
}