package de.employee.service;

import de.employee.management.employee.dto.EmployeeRequest;
import de.employee.management.employee.dto.EmployeeResponse;
import de.employee.management.employee.entity.Employee;
import de.employee.management.employee.exception.DuplicateEmailException;
import de.employee.management.employee.exception.EmployeeNotFoundException;
import de.employee.management.employee.repository.EmployeeRepository;
import de.employee.management.employee.service.EmployeeService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testet die Geschäftslogik des EmployeeService isoliert.
 *
 * Das Repository wird durch Mockito simuliert, sodass
 * für diese Tests keine echte Datenbank benötigt wird.
 *
 * Die Tests überprüfen erfolgreiche Geschäftsabläufe
 * sowie das Verhalten bei fachlichen Fehlern.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService employeeService;

    /**
     * Prüft, ob alle Mitarbeiter korrekt zurückgegeben werden.
     */
    @Test
    void shouldFindAllEmployees() {

        // Zwei Mitarbeiter als Testdaten vorbereiten.
        Employee firstEmployee = createEmployee(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de"
        );

        Employee secondEmployee = createEmployee(
                "PH-1002",
                "Max",
                "Mustermann",
                "max.mustermann@beispiel.de"
        );

        // Das Repository liefert beide Mitarbeiter zurück.
        when(repository.findAll())
                .thenReturn(List.of(firstEmployee, secondEmployee));

        // Die zu testende Methode aufrufen.
        List<EmployeeResponse> responses = employeeService.findAll();

        // Anzahl und Inhalte der zurückgegebenen Mitarbeiter prüfen.
        assertEquals(2, responses.size());
        assertEquals("PH-1001", responses.get(0).id());
        assertEquals("Lena", responses.get(0).firstName());
        assertEquals("PH-1002", responses.get(1).id());
        assertEquals("Max", responses.get(1).firstName());

        // Prüfen, ob das Repository aufgerufen wurde.
        verify(repository).findAll();
    }

    /**
     * Prüft, ob ein vorhandener Mitarbeiter korrekt
     * anhand seiner ID gefunden und zurückgegeben wird.
     */
    @Test
    void shouldFindEmployeeById() {

        // Einen vorhandenen Mitarbeiter vorbereiten.
        Employee employee = createEmployee(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de"
        );

        // Das Repository liefert den Mitarbeiter zurück.
        when(repository.findById("PH-1001"))
                .thenReturn(Optional.of(employee));

        // Die zu testende Methode aufrufen.
        EmployeeResponse response = employeeService.findById("PH-1001");

        // Zurückgegebene Daten überprüfen.
        assertEquals("PH-1001", response.id());
        assertEquals("Lena", response.firstName());
        assertEquals("Hoffmann", response.lastName());
        assertEquals("lena.hoffmann@beispiel.de", response.email());

        // Prüfen, ob das Repository aufgerufen wurde.
        verify(repository).findById("PH-1001");
    }

    /**
     * Prüft, ob beim Suchen eines nicht vorhandenen
     * Mitarbeiters die passende Exception ausgelöst wird.
     */
    @Test
    void shouldThrowExceptionWhenEmployeeDoesNotExist() {

        // Das Repository findet keinen Mitarbeiter mit dieser ID.
        when(repository.findById("PH-9999"))
                .thenReturn(Optional.empty());

        // Prüfen, ob die erwartete Exception ausgelöst wird.
        assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.findById("PH-9999")
        );

        // Prüfen, ob das Repository aufgerufen wurde.
        verify(repository).findById("PH-9999");
    }

    /**
     * Prüft, ob ein neuer Mitarbeiter erfolgreich
     * angelegt und mit den richtigen Daten zurückgegeben wird.
     */
    @Test
    void shouldCreateEmployeeSuccessfully() {

        // Eingabedaten für den neuen Mitarbeiter vorbereiten.
        EmployeeRequest request = createRequest(
                "Max",
                "Mustermann",
                "max.mustermann@beispiel.de"
        );

        // Das Repository meldet, dass die E-Mail noch nicht existiert.
        when(repository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(false);

        // Für die ID-Ermittlung werden noch keine Mitarbeiter gefunden.
        when(repository.findAll())
                .thenReturn(List.of());

        // Das Repository liefert den gespeicherten Mitarbeiter zurück.
        when(repository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Die zu testende Methode aufrufen.
        EmployeeResponse response = employeeService.create(request);

        // Zurückgegebene Daten überprüfen.
        assertEquals("PH-1001", response.id());
        assertEquals("Max", response.firstName());
        assertEquals("Mustermann", response.lastName());
        assertEquals("max.mustermann@beispiel.de", response.email());
        assertEquals("IT", response.department());
        assertEquals("Developer", response.role());
        assertEquals("Köln", response.location());
        assertEquals(LocalDate.of(2024, 1, 15), response.startDate());
        assertEquals("Aktiv", response.status());

        // Prüfen, ob das Repository zum Speichern aufgerufen wurde.
        verify(repository).save(any(Employee.class));
    }

    /**
     * Prüft, ob Mitarbeiter mit einer bereits verwendeten
     * E-Mail-Adresse nicht erneut angelegt werden können.
     */
    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {

        // Einen neuen Mitarbeiter mit einer vorhandenen E-Mail vorbereiten.
        EmployeeRequest request = createRequest(
                "Max",
                "Mustermann",
                "lena.hoffmann@beispiel.de"
        );

        // Das Repository meldet, dass die E-Mail bereits existiert.
        when(repository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(true);

        // Prüfen, ob die erwartete Exception ausgelöst wird.
        assertThrows(
                DuplicateEmailException.class,
                () -> employeeService.create(request)
        );

        // Bei einer doppelten E-Mail darf nicht gespeichert werden.
        verify(repository, never()).save(any(Employee.class));
    }

    /**
     * Prüft, ob ein vorhandener Mitarbeiter erfolgreich
     * aktualisiert und mit den neuen Daten zurückgegeben wird.
     */
    @Test
    void shouldUpdateEmployeeSuccessfully() {

        // Einen vorhandenen Mitarbeiter vorbereiten.
        Employee employee = createEmployee(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de"
        );

        // Neue Mitarbeiterdaten mit einer neuen E-Mail vorbereiten.
        EmployeeRequest request = createRequest(
                "Lena",
                "Schneider",
                "lena.schneider@beispiel.de"
        );

        // Das Repository findet den vorhandenen Mitarbeiter.
        when(repository.findById("PH-1001"))
                .thenReturn(Optional.of(employee));

        // Die neue E-Mail-Adresse ist noch nicht vergeben.
        when(repository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(false);

        // Das Repository liefert den aktualisierten Mitarbeiter zurück.
        when(repository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Die zu testende Methode aufrufen.
        EmployeeResponse response =
                employeeService.update("PH-1001", request);

        // Aktualisierte Daten überprüfen.
        assertEquals("PH-1001", response.id());
        assertEquals("Lena", response.firstName());
        assertEquals("Schneider", response.lastName());
        assertEquals("lena.schneider@beispiel.de", response.email());

        // Prüfen, ob der Mitarbeiter gespeichert wurde.
        verify(repository).save(employee);
    }

    /**
     * Prüft, ob eine bereits von einem anderen Mitarbeiter
     * verwendete E-Mail-Adresse bei einer Aktualisierung
     * abgelehnt wird.
     */
    @Test
    void shouldThrowExceptionWhenUpdatingWithDuplicateEmail() {

        // Einen vorhandenen Mitarbeiter vorbereiten.
        Employee employee = createEmployee(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de"
        );

        // Die neue E-Mail-Adresse ist bereits vergeben.
        EmployeeRequest request = createRequest(
                "Lena",
                "Schneider",
                "max.mustermann@beispiel.de"
        );

        // Das Repository findet den vorhandenen Mitarbeiter.
        when(repository.findById("PH-1001"))
                .thenReturn(Optional.of(employee));

        // Das Repository meldet eine bereits verwendete E-Mail.
        when(repository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(true);

        // Prüfen, ob die erwartete Exception ausgelöst wird.
        assertThrows(
                DuplicateEmailException.class,
                () -> employeeService.update("PH-1001", request)
        );

        // Bei einer doppelten E-Mail darf nicht gespeichert werden.
        verify(repository, never()).save(any(Employee.class));
    }

    /**
     * Prüft, ob ein Mitarbeiter auch dann aktualisiert werden kann,
     * wenn seine bisherige E-Mail-Adresse unverändert bleibt.
     */
    @Test
    void shouldUpdateEmployeeWithoutChangingEmail() {

        // Einen vorhandenen Mitarbeiter vorbereiten.
        Employee employee = createEmployee(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de"
        );

        // Die E-Mail bleibt unverändert, der Nachname ändert sich.
        EmployeeRequest request = createRequest(
                "Lena",
                "Schneider",
                "lena.hoffmann@beispiel.de"
        );

        // Das Repository findet den vorhandenen Mitarbeiter.
        when(repository.findById("PH-1001"))
                .thenReturn(Optional.of(employee));

        // Das Repository liefert den aktualisierten Mitarbeiter zurück.
        when(repository.save(any(Employee.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Die zu testende Methode aufrufen.
        EmployeeResponse response =
                employeeService.update("PH-1001", request);

        // Prüfen, ob die Aktualisierung erfolgreich war.
        assertEquals("Schneider", response.lastName());
        assertEquals("lena.hoffmann@beispiel.de", response.email());

        // Bei unveränderter E-Mail darf keine Duplikatprüfung erfolgen.
        verify(repository, never())
                .existsByEmailIgnoreCase(request.email());

        // Prüfen, ob der Mitarbeiter gespeichert wurde.
        verify(repository).save(employee);
    }

    /**
     * Prüft, ob ein vorhandener Mitarbeiter erfolgreich
     * gelöscht wird.
     */
    @Test
    void shouldDeleteEmployeeSuccessfully() {

        // Einen vorhandenen Mitarbeiter vorbereiten.
        Employee employee = createEmployee(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de"
        );

        // Das Repository liefert den Mitarbeiter zurück.
        when(repository.findById("PH-1001"))
                .thenReturn(Optional.of(employee));

        // Die zu testende Methode aufrufen.
        employeeService.delete("PH-1001");

        // Prüfen, ob genau der gefundene Mitarbeiter gelöscht wird.
        verify(repository).delete(employee);
    }

    /**
     * Prüft, ob beim Löschen eines nicht vorhandenen
     * Mitarbeiters die passende Exception ausgelöst wird.
     */
    @Test
    void shouldThrowExceptionWhenDeletingNonExistingEmployee() {

        // Das Repository findet keinen Mitarbeiter mit dieser ID.
        when(repository.findById("PH-9999"))
                .thenReturn(Optional.empty());

        // Prüfen, ob die erwartete Exception ausgelöst wird.
        assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.delete("PH-9999")
        );

        // Ein nicht vorhandener Mitarbeiter darf nicht gelöscht werden.
        verify(repository, never()).delete(any(Employee.class));
    }

    /**
     * Erstellt einen Mitarbeiter für die Testfälle.
     *
     * Dadurch werden wiederkehrende Testdaten zentral verwaltet.
     */
    private Employee createEmployee(
            String id,
            String firstName,
            String lastName,
            String email
    ) {
        return new Employee(
                id,
                firstName,
                lastName,
                email,
                "IT",
                "Developer",
                "Köln",
                LocalDate.of(2021, 3, 15),
                "Aktiv"
        );
    }

    /**
     * Erstellt ein Request-DTO für die Testfälle.
     */
    private EmployeeRequest createRequest(
            String firstName,
            String lastName,
            String email
    ) {
        return new EmployeeRequest(
                firstName,
                lastName,
                email,
                "IT",
                "Developer",
                "Köln",
                LocalDate.of(2024, 1, 15),
                "Aktiv"
        );
    }
}