
package de.employee.management.exception;

import de.employee.management.employee.exception.DuplicateEmailException;
import de.employee.management.employee.exception.EmployeeNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Fehlerbehandlung für die REST-API.
 *
 * Übersetzt fachliche Exceptions und Validierungsfehler
 * in einheitliche HTTP-Fehlerantworten.
 *
 * Dadurch bleibt die Fehlerbehandlung von den einzelnen
 * Controllern getrennt.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Behandelt Fehler, wenn ein Mitarbeiter nicht existiert.
     */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeNotFound(
            EmployeeNotFoundException exception
    ) {
        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    /**
     * Behandelt doppelt vergebene E-Mail-Adressen.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(
            DuplicateEmailException exception
    ) {
        return createErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    /**
     * Behandelt Fehler bei der Validierung von Request-Daten.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException exception
    ) {
        Map<String, Object> errors = new LinkedHashMap<>();

        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Bad Request");

        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        errors.put("message", "Die übermittelten Daten sind ungültig.");
        errors.put("validationErrors", validationErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    /**
     * Erstellt ein einheitliches Format für Fehlerantworten.
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);

        return ResponseEntity.status(status).body(error);
    }
}
