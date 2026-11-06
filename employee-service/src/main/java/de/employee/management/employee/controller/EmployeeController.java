package de.employee.management.employee.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.employee.management.employee.dto.EmployeeRequest;
import de.employee.management.employee.dto.EmployeeResponse;
import de.employee.management.employee.service.EmployeeService;

/**
 * Stellt die REST-Endpunkte für die Mitarbeiterverwaltung bereit.
 *
 * Der Controller nimmt HTTP-Anfragen entgegen und delegiert
 * die Verarbeitung an den EmployeeService.
 *
 * Die Zugriffsrechte werden anhand der Benutzerrollen geprüft.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Liefert alle Mitarbeiter zurück.
     *
     * ADMIN und USER dürfen Mitarbeiterdaten lesen.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.findAll();
    }

    /**
     * Liefert einen Mitarbeiter anhand seiner ID zurück.
     *
     * ADMIN und USER dürfen einzelne Mitarbeiter abrufen.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public EmployeeResponse getEmployee(@PathVariable String id) {
        return employeeService.findById(id);
    }

    /**
     * Legt einen neuen Mitarbeiter an.
     *
     * Ausschließlich ADMIN darf Mitarbeiter anlegen.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse createEmployee(
            @Valid @RequestBody EmployeeRequest request
    ) {
        return employeeService.create(request);
    }

    /**
     * Aktualisiert einen bestehenden Mitarbeiter.
     *
     * Ausschließlich ADMIN darf Mitarbeiter bearbeiten.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody EmployeeRequest request
    ) {
        return employeeService.update(id, request);
    }

    /**
     * Löscht einen Mitarbeiter anhand seiner ID.
     *
     * Ausschließlich ADMIN darf Mitarbeiter löschen.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEmployee(@PathVariable String id) {
        employeeService.delete(id);
    }
}
