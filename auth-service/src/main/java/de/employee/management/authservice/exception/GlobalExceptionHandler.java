package de.employee.management.authservice.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Zentrale Fehlerbehandlung des Auth-Service.
 *
 * Vereinheitlicht die HTTP-Fehlerantworten für alle REST-Endpunkte.
 * Behandelt insbesondere fehlgeschlagene Anmeldeversuche,
 * ungültige Eingabedaten und unerwartete Serverfehler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Behandelt fehlgeschlagene Authentifizierungsversuche.
     *
     * Die Fehlermeldung unterscheidet bewusst nicht zwischen
     * einer unbekannten E-Mail-Adresse und einem falschen Passwort.
     *
     * @param exception aufgetretener Authentifizierungsfehler
     * @return HTTP 401 mit einer allgemeinen Fehlermeldung
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException exception) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "E-Mail-Adresse oder Passwort ist ungültig."
        );
    }

    /**
     * Behandelt Validierungsfehler bei eingehenden REST-Anfragen.
     *
     * Die einzelnen Feldfehler werden gesammelt und in der Antwort
     * unter validationErrors zurückgegeben.
     *
     * @param exception aufgetretener Validierungsfehler
     * @return HTTP 400 mit den betroffenen Feldern
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> validationErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                error -> error.getField(),
                                error -> error.getDefaultMessage() != null
                                        ? error.getDefaultMessage()
                                        : "Ungültiger Wert.",
                                (firstError, secondError) -> firstError
                        ));

        Map<String, Object> response = createErrorBody(
                HttpStatus.BAD_REQUEST,
                "Die übermittelten Daten sind ungültig."
        );

        response.put("validationErrors", validationErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Behandelt nicht lesbare oder fehlerhaft formatierte Request-Bodies.
     *
     * @param exception aufgetretener Fehler beim Lesen der Anfrage
     * @return HTTP 400 mit einer allgemeinen Fehlermeldung
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Die Anfrage enthält ungültige oder nicht lesbare Daten."
        );
    }

    /**
     * Behandelt alle nicht explizit definierten Fehler.
     *
     * Interne technische Details werden nicht an den Client übertragen.
     *
     * @param exception aufgetretener unerwarteter Fehler
     * @return HTTP 500 mit einer allgemeinen Fehlermeldung
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception) {

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ein interner Serverfehler ist aufgetreten."
        );
    }

    /**
     * Erstellt eine standardisierte Fehlerantwort.
     *
     * @param status HTTP-Status der Fehlerantwort
     * @param message verständliche Fehlermeldung
     * @return HTTP-Antwort mit standardisiertem Fehlerobjekt
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(createErrorBody(status, message));
    }

    /**
     * Erstellt den gemeinsamen Inhalt einer Fehlerantwort.
     *
     * @param status HTTP-Status der Fehlerantwort
     * @param message verständliche Fehlermeldung
     * @return Fehlerdaten für die REST-Antwort
     */
    private Map<String, Object> createErrorBody(
            HttpStatus status,
            String message) {

        return new java.util.LinkedHashMap<>(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}
