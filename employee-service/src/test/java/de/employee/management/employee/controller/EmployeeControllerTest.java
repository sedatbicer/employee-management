package de.employee.management.employee.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.employee.management.employee.dto.EmployeeRequest;
import de.employee.management.employee.dto.EmployeeResponse;
import de.employee.management.employee.service.EmployeeService;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;


/*
 * Diese Testklasse überprüft den EmployeeController.
 *
 * Dabei testen wir:
 * - Ob die HTTP-Endpunkte korrekt funktionieren.
 * - Ob die richtigen HTTP-Statuscodes zurückgegeben werden.
 * - Ob die JSON-Antwort die erwarteten Daten enthält.
 * - Ob Benutzer mit den richtigen Rollen auf die Endpunkte zugreifen dürfen.
 *
 * @WebMvcTest lädt nur die Web-Schicht der Anwendung.
 * Dadurch testen wir den Controller isoliert.
 *
 * @Import bindet unsere Testkonfiguration für die Method Security ein.
 * Dadurch werden auch die @PreAuthorize-Annotationen des Controllers geprüft.
 */
@WebMvcTest(controllers = EmployeeController.class)
@Import(EmployeeControllerTest.MethodSecurityTestConfig.class)
class EmployeeControllerTest {


    /*
     * Testkonfiguration für die rollenbasierte Zugriffskontrolle.
     *
     * @EnableMethodSecurity aktiviert die Auswertung von Annotationen
     * wie @PreAuthorize.
     *
     * Dadurch können wir testen, ob beispielsweise ein USER
     * tatsächlich daran gehindert wird, Mitarbeiter zu erstellen.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }


    /*
     * MockMvc ermöglicht es uns, HTTP-Anfragen an den Controller zu senden,
     * ohne dafür einen echten Server starten zu müssen.
     *
     * Damit können wir GET-, POST-, PUT- und DELETE-Anfragen simulieren
     * und die jeweiligen HTTP-Antworten überprüfen.
     */
    @Autowired
    private MockMvc mockMvc;


    /*
     * Der EmployeeService wird durch einen Mock ersetzt.
     *
     * Dadurch testen wir ausschließlich den Controller.
     * Die eigentliche Geschäftslogik des Services wird nicht ausgeführt.
     *
     * Mit Mockito legen wir fest, welche Ergebnisse der Service
     * bei bestimmten Methodenaufrufen zurückgeben soll.
     */
    @MockitoBean
    private EmployeeService employeeService;


    /*
     * Hilfsmethode zum Erstellen eines Beispiel-Mitarbeiters.
     *
     * Dadurch müssen wir in den einzelnen Tests nicht immer wieder
     * ein neues EmployeeResponse-Objekt erstellen.
     *
     * Der Mitarbeiter wird als erwartete Antwort verwendet.
     */
    private EmployeeResponse createEmployeeResponse() {

        return new EmployeeResponse(
                "PH-1001",
                "Lena",
                "Hoffmann",
                "lena.hoffmann@beispiel.de",
                "IT",
                "Developer",
                "Köln",
                LocalDate.of(2021, 3, 15),
                "Aktiv"
        );
    }


    /*
     * Hilfsmethode zum Erstellen eines JSON-Requests.
     *
     * Dieses JSON simuliert die Mitarbeiterdaten,
     * die ein Client beim Erstellen oder Aktualisieren
     * eines Mitarbeiters an den Controller senden würde.
     */
    private String createEmployeeJson() {

        return """
                {
                    "firstName": "Lena",
                    "lastName": "Hoffmann",
                    "email": "lena.hoffmann@beispiel.de",
                    "department": "IT",
                    "role": "Developer",
                    "location": "Köln",
                    "startDate": "2021-03-15",
                    "status": "Aktiv"
                }
                """;
    }


    /*
     * TEST 1:
     * Alle Mitarbeiter abrufen.
     *
     * Ein Benutzer mit der Rolle USER darf Mitarbeiter lesen.
     *
     * Wir simulieren, dass der Service eine Liste mit einem
     * Mitarbeiter zurückgibt.
     *
     * Anschließend überprüfen wir:
     * - HTTP-Status 200 OK
     * - Die Antwort enthält genau einen Mitarbeiter.
     * - Die wichtigsten Mitarbeiterdaten sind korrekt.
     */
    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnAllEmployees() throws Exception {

        // Der Service soll eine Liste mit einem Mitarbeiter zurückgeben.
        when(employeeService.findAll())
                .thenReturn(List.of(createEmployeeResponse()));

        // GET-Anfrage zum Abrufen aller Mitarbeiter senden.
        mockMvc.perform(get("/api/employees"))

                // Die Anfrage muss erfolgreich sein.
                .andExpect(status().isOk())

                // Überprüfen, ob genau ein Mitarbeiter zurückgegeben wurde.
                .andExpect(jsonPath("$.length()").value(1))

                // Die wichtigsten Eigenschaften des Mitarbeiters überprüfen.
                .andExpect(jsonPath("$[0].id").value("PH-1001"))
                .andExpect(jsonPath("$[0].firstName").value("Lena"))
                .andExpect(jsonPath("$[0].lastName").value("Hoffmann"))
                .andExpect(jsonPath("$[0].email")
                        .value("lena.hoffmann@beispiel.de"));
    }


    /*
     * TEST 2:
     * Einen einzelnen Mitarbeiter anhand seiner ID abrufen.
     *
     * Ein Benutzer mit der Rolle USER darf diesen Endpunkt verwenden.
     *
     * Wir simulieren, dass der Service den gesuchten Mitarbeiter findet.
     *
     * Danach überprüfen wir:
     * - HTTP-Status 200 OK
     * - Die zurückgegebenen Mitarbeiterdaten sind korrekt.
     * - Der Service wurde mit der richtigen ID aufgerufen.
     */
    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnEmployeeById() throws Exception {

        // Der Service soll für diese ID den Beispiel-Mitarbeiter zurückgeben.
        when(employeeService.findById("PH-1001"))
                .thenReturn(createEmployeeResponse());

        // GET-Anfrage mit der Mitarbeiter-ID senden.
        mockMvc.perform(get("/api/employees/PH-1001"))

                // Die Anfrage muss erfolgreich sein.
                .andExpect(status().isOk())

                // Die zurückgegebenen Mitarbeiterdaten überprüfen.
                .andExpect(jsonPath("$.id").value("PH-1001"))
                .andExpect(jsonPath("$.firstName").value("Lena"))
                .andExpect(jsonPath("$.email")
                        .value("lena.hoffmann@beispiel.de"));

        // Sicherstellen, dass der Service mit der richtigen ID aufgerufen wurde.
        verify(employeeService).findById("PH-1001");
    }


    /*
     * TEST 3:
     * Einen neuen Mitarbeiter erstellen.
     *
     * Nur ein Benutzer mit der Rolle ADMIN darf Mitarbeiter erstellen.
     *
     * Wir simulieren, dass der Service den neuen Mitarbeiter erfolgreich
     * erstellt und als EmployeeResponse zurückgibt.
     *
     * Danach überprüfen wir:
     * - HTTP-Status 201 CREATED
     * - Die zurückgegebenen Mitarbeiterdaten sind korrekt.
     * - Die create-Methode des Services wurde aufgerufen.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateEmployee() throws Exception {

        // Der Service soll nach dem Erstellen den Beispiel-Mitarbeiter zurückgeben.
        when(employeeService.create(any(EmployeeRequest.class)))
                .thenReturn(createEmployeeResponse());

        // POST-Anfrage mit den Mitarbeiterdaten als JSON senden.
        mockMvc.perform(post("/api/employees")

                        // CSRF-Token für die POST-Anfrage hinzufügen.
                        .with(csrf())

                        // Angeben, dass der Request JSON-Daten enthält.
                        .contentType(MediaType.APPLICATION_JSON)

                        // Die Mitarbeiterdaten als JSON übertragen.
                        .content(createEmployeeJson()))

                // Beim erfolgreichen Erstellen erwarten wir HTTP 201.
                .andExpect(status().isCreated())

                // Überprüfen, ob die erwarteten Daten zurückgegeben werden.
                .andExpect(jsonPath("$.id").value("PH-1001"))
                .andExpect(jsonPath("$.firstName").value("Lena"))
                .andExpect(jsonPath("$.email")
                        .value("lena.hoffmann@beispiel.de"));

        // Sicherstellen, dass der Service zum Erstellen aufgerufen wurde.
        verify(employeeService).create(any(EmployeeRequest.class));
    }


    /*
     * TEST 4:
     * Einen bestehenden Mitarbeiter aktualisieren.
     *
     * Nur ein ADMIN darf Mitarbeiterdaten verändern.
     *
     * Wir simulieren, dass der Service den Mitarbeiter erfolgreich aktualisiert.
     *
     * Danach überprüfen wir:
     * - HTTP-Status 200 OK
     * - Die zurückgegebenen Daten sind korrekt.
     * - Der Service wurde mit der richtigen ID aufgerufen.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateEmployee() throws Exception {

        // Der Service soll den aktualisierten Mitarbeiter zurückgeben.
        when(employeeService.update(
                eq("PH-1001"),
                any(EmployeeRequest.class)))
                .thenReturn(createEmployeeResponse());

        // PUT-Anfrage mit ID und neuen Mitarbeiterdaten senden.
        mockMvc.perform(put("/api/employees/PH-1001")

                        // CSRF-Token für die PUT-Anfrage hinzufügen.
                        .with(csrf())

                        // JSON als Datenformat angeben.
                        .contentType(MediaType.APPLICATION_JSON)

                        // Die neuen Mitarbeiterdaten übertragen.
                        .content(createEmployeeJson()))

                // Eine erfolgreiche Aktualisierung liefert HTTP 200.
                .andExpect(status().isOk())

                // Die Antwort auf die erwarteten Daten überprüfen.
                .andExpect(jsonPath("$.id").value("PH-1001"))
                .andExpect(jsonPath("$.firstName").value("Lena"));

        // Prüfen, ob der Service mit der richtigen ID aufgerufen wurde.
        verify(employeeService).update(
                eq("PH-1001"),
                any(EmployeeRequest.class));
    }


    /*
     * TEST 5:
     * Einen Mitarbeiter löschen.
     *
     * Nur ein ADMIN darf Mitarbeiter löschen.
     *
     * Da die delete-Methode keine Antwort zurückgibt,
     * überprüfen wir den HTTP-Status und den Service-Aufruf.
     *
     * Erwartet wird HTTP 204 NO CONTENT.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteEmployee() throws Exception {

        // DELETE-Anfrage mit der ID des Mitarbeiters senden.
        mockMvc.perform(delete("/api/employees/PH-1001")
                        .with(csrf()))

                // Erfolgreiches Löschen liefert HTTP 204 ohne Antwortinhalt.
                .andExpect(status().isNoContent());

        // Sicherstellen, dass der Service zum Löschen aufgerufen wurde.
        verify(employeeService).delete("PH-1001");
    }


    /*
     * TEST 6:
     * Überprüfen, dass ein USER keinen Mitarbeiter erstellen darf.
     *
     * Der Zugriff muss mit HTTP 403 FORBIDDEN abgelehnt werden.
     *
     * Zusätzlich prüfen wir, dass der Service nicht aufgerufen wird.
     * So stellen wir sicher, dass keine unerlaubte Erstellung stattfindet.
     */
    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidEmployeeCreationForUser() throws Exception {

        // Ein USER versucht, einen neuen Mitarbeiter anzulegen.
        mockMvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson()))

                // Der Zugriff muss verweigert werden.
                .andExpect(status().isForbidden());

        // Der Service darf bei einem unerlaubten Zugriff nicht aufgerufen werden.
        verify(employeeService, never())
                .create(any(EmployeeRequest.class));
    }


    /*
     * TEST 7:
     * Überprüfen, dass ein USER keinen Mitarbeiter aktualisieren darf.
     *
     * Die PUT-Anfrage muss mit HTTP 403 FORBIDDEN abgelehnt werden.
     *
     * Außerdem stellen wir sicher, dass der Service
     * keine Aktualisierung durchgeführt hat.
     */
    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidEmployeeUpdateForUser() throws Exception {

        // Ein USER versucht, die Daten eines Mitarbeiters zu verändern.
        mockMvc.perform(put("/api/employees/PH-1001")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson()))

                // Der Zugriff muss verweigert werden.
                .andExpect(status().isForbidden());

        // Der Aktualisierungs-Service darf nicht aufgerufen worden sein.
        verify(employeeService, never())
                .update(anyString(), any(EmployeeRequest.class));
    }


    /*
     * TEST 8:
     * Überprüfen, dass ein USER keinen Mitarbeiter löschen darf.
     *
     * Die DELETE-Anfrage muss mit HTTP 403 FORBIDDEN abgelehnt werden.
     *
     * Zusätzlich prüfen wir, dass die delete-Methode
     * des Services nicht aufgerufen wurde.
     */
    @Test
    @WithMockUser(roles = "USER")
    void shouldForbidEmployeeDeletionForUser() throws Exception {

        // Ein USER versucht, einen Mitarbeiter zu löschen.
        mockMvc.perform(delete("/api/employees/PH-1001")
                        .with(csrf()))

                // Der Zugriff muss verweigert werden.
                .andExpect(status().isForbidden());

        // Der Mitarbeiter darf nicht gelöscht worden sein.
        verify(employeeService, never())
                .delete(anyString());
    }
}
