
package de.employee.management.employee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.employee.management.employee.dto.EmployeeRequest;
import de.employee.management.employee.dto.EmployeeResponse;
import de.employee.management.employee.entity.Employee;
import de.employee.management.employee.exception.DuplicateEmailException;
import de.employee.management.employee.exception.EmployeeNotFoundException;
import de.employee.management.employee.repository.EmployeeRepository;

import java.util.Comparator;
import java.util.List;

/**
 * Enthält die Geschäftslogik für die Verwaltung von Mitarbeitern.
 *
 * Der Service koordiniert den Datenbankzugriff, prüft fachliche
 * Regeln und wandelt Datenbank-Entities in Response-DTOs um.
 */
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    /**
     * Liefert alle Mitarbeiter als Response-DTOs zurück.
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Sucht einen Mitarbeiter anhand seiner ID.
     *
     * @param id Mitarbeiter-ID
     * @return Mitarbeiterdaten als Response-DTO
     */
    @Transactional(readOnly = true)
    public EmployeeResponse findById(String id) {
        Employee employee = findEmployee(id);
        return toResponse(employee);
    }

    /**
     * Legt einen neuen Mitarbeiter an.
     *
     * @param request Eingabedaten des Mitarbeiters
     * @return Gespeicherte Mitarbeiterdaten
     */
    public EmployeeResponse create(EmployeeRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Employee employee = new Employee();
        employee.setId(nextId());

        applyRequest(employee, request);

        Employee savedEmployee = repository.save(employee);

        return toResponse(savedEmployee);
    }

    /**
     * Aktualisiert einen bestehenden Mitarbeiter.
     *
     * @param id Mitarbeiter-ID
     * @param request Neue Mitarbeiterdaten
     * @return Aktualisierte Mitarbeiterdaten
     */
    public EmployeeResponse update(
            String id,
            EmployeeRequest request
    ) {
        Employee employee = findEmployee(id);

        boolean emailChanged = !employee.getEmail()
                .equalsIgnoreCase(request.email());

        if (emailChanged
                && repository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        applyRequest(employee, request);

        Employee updatedEmployee = repository.save(employee);

        return toResponse(updatedEmployee);
    }

    /**
     * Löscht einen Mitarbeiter anhand seiner ID.
     *
     * @param id Mitarbeiter-ID
     */
    public void delete(String id) {
        Employee employee = findEmployee(id);
        repository.delete(employee);
    }

    /**
     * Sucht einen Mitarbeiter und löst bei fehlendem Datensatz
     * eine fachliche Exception aus.
     */
    private Employee findEmployee(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    /**
     * Überträgt die Eingabedaten auf die Datenbank-Entity.
     */
    private void applyRequest(
            Employee employee,
            EmployeeRequest request
    ) {
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        employee.setRole(request.role());
        employee.setLocation(request.location());
        employee.setStartDate(request.startDate());
        employee.setStatus(request.status());
    }

    /**
     * Wandelt eine Datenbank-Entity in ein Response-DTO um.
     */
    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getRole(),
                employee.getLocation(),
                employee.getStartDate(),
                employee.getStatus()
        );
    }

    /**
     * Ermittelt die nächste freie Mitarbeiter-ID.
     *
     * ID-Format PH-1001
     */
    private String nextId() {
        int highest = repository.findAll().stream()
                .map(Employee::getId)
                .filter(id -> id != null && id.matches("PH-\\d+"))
                .map(id -> Integer.parseInt(id.substring(3)))
                .max(Comparator.naturalOrder())
                .orElse(1000);

        return "PH-" + (highest + 1);
    }
}
