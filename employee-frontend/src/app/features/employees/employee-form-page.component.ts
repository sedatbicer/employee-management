import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import {
  EmployeeInput,
  EmployeeStatus
} from '../../core/models/employee.model';

import { EmployeeService } from '../../core/services/employee.service';

/**
 * Formularseite zur Erstellung und Bearbeitung von Mitarbeitern.
 *
 * Die Komponente stellt ein gemeinsames Formular für beide Vorgänge bereit:
 * - Erstellung eines neuen Mitarbeiters
 * - Bearbeitung eines bestehenden Mitarbeiters
 *
 * Die Formularvalidierung erfolgt clientseitig über Angular Reactive Forms.
 * Die Kommunikation mit dem Backend wird über den EmployeeService abgewickelt.
 */
@Component({
  selector: 'app-employee-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],

  template: `
    <section class="page-content form-page">
      <div class="page-heading">
        <div>
          <div class="eyebrow">MITARBEITERDATEN</div>
          <h1>
            {{ employeeId ? 'Mitarbeiter bearbeiten' : 'Mitarbeiter hinzufügen' }}
          </h1>
          <p>Ändere die Stammdaten und speichere deine Angaben.</p>
        </div>
      </div>

      <!-- Formular zur Erstellung oder Bearbeitung eines Mitarbeiters. -->
      <form class="form-card" [formGroup]="form" (ngSubmit)="save()">

        <div class="form-grid">

          <!-- Vorname des Mitarbeiters. -->
          <label class="form-field">
            <span>Vorname *</span>
            <input formControlName="firstName" placeholder="z. B. Anna">

            @if (form.controls.firstName.touched && form.controls.firstName.invalid) {
              <small class="visible-error">
                Mindestens 2 Zeichen eingeben.
              </small>
            }
          </label>

          <!-- Nachname des Mitarbeiters. -->
          <label class="form-field">
            <span>Nachname *</span>
            <input formControlName="lastName" placeholder="z. B. Müller">

            @if (form.controls.lastName.touched && form.controls.lastName.invalid) {
              <small class="visible-error">
                Mindestens 2 Zeichen eingeben.
              </small>
            }
          </label>

          <!-- Geschäftliche E-Mail-Adresse des Mitarbeiters. -->
          <label class="form-field full-field">
            <span>E-Mail-Adresse *</span>
            <input
              formControlName="email"
              type="email"
              placeholder="anna.mueller@firma.de"
            >

            @if (form.controls.email.touched && form.controls.email.invalid) {
              <small class="visible-error">
                Bitte eine gültige E-Mail-Adresse eingeben.
              </small>
            }
          </label>

          <!-- Organisatorische Zuordnung des Mitarbeiters. -->
          <label class="form-field">
            <span>Abteilung *</span>
            <select formControlName="department">
              @for (department of departments; track department) {
                <option [value]="department">{{ department }}</option>
              }
            </select>
          </label>

          <!-- Berufliche Position des Mitarbeiters. -->
          <label class="form-field">
            <span>Position *</span>
            <input
              formControlName="role"
              placeholder="z. B. Projektmanagerin"
            >
          </label>

          <!-- Standort des Mitarbeiters. -->
          <label class="form-field">
            <span>Standort *</span>
            <select formControlName="location">
              @for (location of locations; track location) {
                <option [value]="location">{{ location }}</option>
              }
            </select>
          </label>

          <!-- Eintrittsdatum des Mitarbeiters. -->
          <label class="form-field">
            <span>Eintrittsdatum *</span>
            <input formControlName="startDate" type="date">
          </label>

          <!-- Aktueller Beschäftigungsstatus. -->
          <label class="form-field">
            <span>Status *</span>
            <select formControlName="status">
              <option value="Aktiv">Aktiv</option>
              <option value="Urlaub">Urlaub</option>
              <option value="Inaktiv">Inaktiv</option>
            </select>
          </label>

        </div>

        <!-- Allgemeine Fehlermeldung bei fehlgeschlagenen Backend-Anfragen. -->
        @if (errorMessage()) {
          <p class="visible-error">{{ errorMessage() }}</p>
        }

        <!-- Aktionen zum Abbrechen oder Speichern des Formulars. -->
        <div class="form-actions">
          <a class="secondary-button" routerLink="/mitarbeiter">
            Abbrechen
          </a>

          <button
            class="primary-button"
            type="submit"
            [disabled]="loading()"
          >
            @if (loading()) {
              Bitte warten ...
            } @else {
              {{ employeeId ? 'Änderungen speichern' : 'Mitarbeiter anlegen' }}
            }
          </button>
        </div>

      </form>
    </section>
  `,

  styles: [`
    .form-page { max-width:1050px; }

    .form-card {
      background:#fff;
      border:1px solid #e9edf4;
      border-radius:14px;
      padding:28px;
    }

    .form-grid {
      display:grid;
      grid-template-columns:repeat(2,minmax(0,1fr));
      gap:20px;
    }

    .form-field {
      display:flex;
      flex-direction:column;
      gap:8px;
      color:#34415a;
      font-size:13px;
      font-weight:600;
    }

    .form-field input,
    .form-field select {
      width:100%;
      border:1px solid #dce2ec;
      border-radius:8px;
      padding:12px;
      background:#fff;
      color:#263247;
      font:inherit;
      font-weight:400;
    }

    .full-field {
      grid-column:1/-1;
    }

    .form-field small {
      min-height:15px;
      color:#c0392b;
      visibility:hidden;
      font-weight:400;
    }

    .form-field small.visible-error {
      visibility:visible;
    }

    .form-actions {
      display:flex;
      justify-content:flex-end;
      gap:12px;
      margin-top:28px;
    }

    .secondary-button {
      display:inline-flex;
      align-items:center;
      padding:11px 16px;
      border-radius:8px;
      background:#f0f3f8;
      color:#34415a;
      text-decoration:none;
    }

    @media(max-width:650px) {
      .form-grid {
        grid-template-columns:1fr;
      }

      .full-field {
        grid-column:auto;
      }

      .form-card {
        padding:18px;
      }
    }
  `]
})
export class EmployeeFormPageComponent implements OnInit {

  /** Angular-Dienste für Formularverwaltung, Navigation und Datenzugriff. */
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);

  /**
   * ID des zu bearbeitenden Mitarbeiters.
   *
   * Ist keine ID in der Route vorhanden, wird das Formular
   * im Erstellungsmodus verwendet.
   */
  readonly employeeId = this.route.snapshot.paramMap.get('id');

  /** Steuert den Ladezustand während der Backend-Kommunikation. */
  readonly loading = signal(false);

  /** Enthält eine allgemeine Fehlermeldung für den Benutzer. */
  readonly errorMessage = signal('');

  /**
   * Verfügbare Abteilungen für die Auswahl im Formular.
   *
   * Die Einträge werden aktuell statisch bereitgestellt.
   */
  readonly departments = [
    'Engineering',
    'Personal',
    'Design',
    'Finanzen',
    'Marketing',
    'Vertrieb',
    'Produkt'
  ];

  /**
   * Verfügbare Standorte für die Auswahl im Formular.
   *
   * Die Einträge werden aktuell statisch bereitgestellt.
   */
  readonly locations = [
    'Köln',
    'Berlin',
    'Hamburg',
    'München',
    'Frankfurt',
    'Düsseldorf',
    'Remote'
  ];

  /**
   * Reaktives Formular für die Mitarbeiterdaten.
   *
   * Alle Formularfelder sind verpflichtend.
   * Zusätzlich werden Vorname, Nachname und E-Mail-Adresse
   * anhand definierter Validierungsregeln geprüft.
   *
   * Standardwerte werden für Abteilung, Standort, Eintrittsdatum
   * und Beschäftigungsstatus festgelegt.
   */
  readonly form = this.fb.nonNullable.group({
    firstName: ['', [
      Validators.required,
      Validators.minLength(2)
    ]],

    lastName: ['', [
      Validators.required,
      Validators.minLength(2)
    ]],

    email: ['', [
      Validators.required,
      Validators.email
    ]],

    department: ['Engineering', Validators.required],
    role: ['', Validators.required],
    location: ['Köln', Validators.required],

    startDate: [
      new Date().toISOString().slice(0, 10),
      Validators.required
    ],

    status: ['Aktiv' as EmployeeStatus, Validators.required]
  });

  /**
   * Initialisiert die Formularseite.
   *
   * Befindet sich eine Mitarbeiter-ID in der Route,
   * werden die entsprechenden Mitarbeiterdaten vom Backend geladen.
   */
  ngOnInit(): void {
    if (this.employeeId) {
      this.loadEmployee(this.employeeId);
    }
  }

  /**
   * Lädt die Daten eines bestehenden Mitarbeiters.
   *
   * Die empfangenen Daten werden anschließend in das Formular übernommen.
   * Während der Anfrage wird der Ladezustand aktiviert.
   *
   * @param id Eindeutige ID des Mitarbeiters
   */
  private loadEmployee(id: string): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.employeeService.findById(id).subscribe({
      next: employee => {
        // Geladene Mitarbeiterdaten in die Formularfelder übernehmen.
        this.form.patchValue(employee);

        this.loading.set(false);
      },

      error: error => {
        // Technische Fehler für die Entwicklung protokollieren.
        console.error(
          'Mitarbeiter konnte nicht geladen werden:',
          error
        );

        this.loading.set(false);
        this.errorMessage.set(
          'Mitarbeiter konnte nicht geladen werden.'
        );
      }
    });
  }

  /**
   * Speichert die eingegebenen Mitarbeiterdaten.
   *
   * Abhängig vom Vorhandensein einer Mitarbeiter-ID wird
   * entweder ein neuer Mitarbeiter angelegt oder ein bestehender
   * Mitarbeiter aktualisiert.
   *
   * Vor der Übermittlung werden die Formularwerte validiert.
   * Bei erfolgreicher Speicherung erfolgt die Weiterleitung
   * zur Mitarbeiterübersicht.
   */
  save(): void {
    // Ungültige Formulareingaben kennzeichnen und Speicherung verhindern.
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    // Mehrfache Speicheranfragen verhindern.
    if (this.loading()) {
      return;
    }

    // Validierte Formularwerte für die Backend-Anfrage übernehmen.
    const input: EmployeeInput = this.form.getRawValue();

    this.loading.set(true);
    this.errorMessage.set('');

    // Je nach Formularmodus den passenden Service-Aufruf auswählen.
    const request$ = this.employeeId
      ? this.employeeService.update(this.employeeId, input)
      : this.employeeService.create(input);

    request$.subscribe({
      next: () => {
        this.loading.set(false);

        // Nach erfolgreicher Speicherung zur Mitarbeiterübersicht wechseln.
        void this.router.navigate(['/mitarbeiter']);
      },

      error: error => {
        // Technische Fehler für die Entwicklung protokollieren.
        console.error(
          'Mitarbeiter konnte nicht gespeichert werden:',
          error
        );

        this.loading.set(false);
        this.errorMessage.set(
          'Speichern fehlgeschlagen. Bitte überprüfe deine Angaben.'
        );
      }
    });
  }
}