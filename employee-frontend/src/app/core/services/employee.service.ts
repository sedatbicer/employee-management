import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, finalize } from 'rxjs';

import { Employee, EmployeeInput } from '../models/employee.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiEmployee;

  // Zentraler Mitarbeiterzustand innerhalb der Anwendung
  readonly employees = signal<Employee[]>([]);

  // Abgeleitete Anzahl der aktuell gespeicherten Mitarbeiter
  readonly employeeCount = computed(() => this.employees().length);

  // Ladezustand und Fehlermeldung für die Benutzeroberfläche
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /**
   * Lädt alle Mitarbeiter vom Backend.
   * Aktualisiert den zentralen Mitarbeiterzustand.
   */
  loadEmployees(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.http.get<Employee[]>(this.apiUrl)
      .pipe(
        // Ladezustand unabhängig vom Ergebnis zurücksetzen
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (employees) => {
          this.employees.set(employees);
        },
        error: (error) => {
          console.error(
            'Mitarbeiter konnten nicht geladen werden:',
            error
          );

          this.errorMessage.set(
            'Mitarbeiter konnten nicht geladen werden.'
          );
        }
      });
  }

  /**
   * Lädt einen einzelnen Mitarbeiter anhand seiner ID.
   */
  findById(id: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.apiUrl}/${id}`);
  }

  /**
   * Legt einen neuen Mitarbeiter im Backend an.
   * Fügt den zurückgegebenen Mitarbeiter anschließend
   * dem zentralen Mitarbeiterzustand hinzu.
   */
  create(input: EmployeeInput): Observable<Employee> {
    return this.http.post<Employee>(this.apiUrl, input).pipe(
      tap((employee) => {
        this.employees.update(items => [employee, ...items]);
      })
    );
  }

  /**
   * Aktualisiert einen bestehenden Mitarbeiter.
   * Ersetzt den entsprechenden Eintrag im Mitarbeiterzustand.
   */
  update(id: string, input: EmployeeInput): Observable<Employee> {
    return this.http.put<Employee>(
      `${this.apiUrl}/${id}`,
      input
    ).pipe(
      tap((updatedEmployee) => {
        this.employees.update(items =>
          items.map(item =>
            item.id === id ? updatedEmployee : item
          )
        );
      })
    );
  }

  /**
   * Löscht einen Mitarbeiter anhand seiner ID.
   * Entfernt ihn nach erfolgreicher Backend-Antwort
   * auch aus dem zentralen Mitarbeiterzustand.
   */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        this.employees.update(items =>
          items.filter(item => item.id !== id)
        );
      })
    );
  }
}
