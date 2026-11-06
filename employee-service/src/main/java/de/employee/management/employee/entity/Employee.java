
package de.employee.management.employee.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA-Entity zur Abbildung eines Mitarbeiters in der Datenbank.
 *
 * Die Klasse repräsentiert einen Datensatz der Tabelle employees.
 * Sie enthält die Stammdaten eines Mitarbeiters und bildet
 * das interne Persistenzmodell des Employee-Service.
 *
 * Die Entity wird ausschließlich für den Datenbankzugriff verwendet.
 * Für die Kommunikation über die REST-API werden separate DTOs
 * eingesetzt, damit das Datenbankmodell nicht direkt nach außen
 * veröffentlicht wird.
 */
@Entity
@Table(name = "employees")
public class Employee {

    /**
     * Eindeutige Mitarbeiter-ID.
     *
     * Die ID wird vom Backend vergeben und dient als Primärschlüssel.
     * Die maximale Länge ist auf 20 Zeichen begrenzt.
     */
    @Id
    @Column(length = 20, nullable = false)
    private String id;

    /**
     * Vorname des Mitarbeiters.
     *
     * Darf in der Datenbank nicht null sein.
     */
    @Column(nullable = false)
    private String firstName;

    /**
     * Nachname des Mitarbeiters.
     *
     * Darf in der Datenbank nicht null sein.
     */
    @Column(nullable = false)
    private String lastName;

    /**
     * Eindeutige E-Mail-Adresse des Mitarbeiters.
     *
     * Durch unique = true verhindert die Datenbank,
     * dass dieselbe E-Mail-Adresse mehrfach gespeichert wird.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Abteilung, der der Mitarbeiter zugeordnet ist.
     */
    private String department;

    /**
     * Funktion oder berufliche Position des Mitarbeiters.
     */
    private String role;

    /**
     * Standort oder Arbeitsort des Mitarbeiters.
     */
    private String location;

    /**
     * Datum des Beschäftigungsbeginns.
     *
     * LocalDate speichert ein Datum ohne Uhrzeit und Zeitzone.
     */
    private LocalDate startDate;

    /**
     * Aktueller Beschäftigungsstatus des Mitarbeiters.
     *
     * Darf in der Datenbank nicht null sein.
     */
    @Column(nullable = false)
    private String status;

    /**
     * Parameterloser Konstruktor für JPA.
     *
     * JPA benötigt einen öffentlichen oder geschützten
     * parameterlosen Konstruktor, um Entity-Instanzen
     * beim Laden aus der Datenbank erzeugen zu können.
     */
    public Employee() {
    }

    /**
     * Erstellt einen Mitarbeiter mit allen Stammdaten.
     *
     * Dieser Konstruktor wird beispielsweise bei der
     * Erstellung neuer Mitarbeiter im Service verwendet.
     *
     * @param id eindeutige Mitarbeiter-ID
     * @param firstName Vorname
     * @param lastName Nachname
     * @param email E-Mail-Adresse
     * @param department Abteilung
     * @param role berufliche Position
     * @param location Arbeitsort
     * @param startDate Eintrittsdatum
     * @param status Beschäftigungsstatus
     */
    public Employee(
            String id,
            String firstName,
            String lastName,
            String email,
            String department,
            String role,
            String location,
            LocalDate startDate,
            String status
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.role = role;
        this.location = location;
        this.startDate = startDate;
        this.status = status;
    }

    /**
     * Liefert die eindeutige Mitarbeiter-ID zurück.
     *
     * @return Mitarbeiter-ID
     */
    public String getId() {
        return id;
    }

    /**
     * Setzt die Mitarbeiter-ID.
     *
     * @param id neue Mitarbeiter-ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Liefert den Vornamen zurück.
     *
     * @return Vorname
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Setzt den Vornamen.
     *
     * @param firstName neuer Vorname
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Liefert den Nachnamen zurück.
     *
     * @return Nachname
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Setzt den Nachnamen.
     *
     * @param lastName neuer Nachname
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Liefert die E-Mail-Adresse zurück.
     *
     * @return E-Mail-Adresse
     */
    public String getEmail() {
        return email;
    }

    /**
     * Setzt die E-Mail-Adresse.
     *
     * @param email neue E-Mail-Adresse
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Liefert die Abteilung zurück.
     *
     * @return Abteilung
     */
    public String getDepartment() {
        return department;
    }

    /**
     * Setzt die Abteilung.
     *
     * @param department neue Abteilung
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * Liefert die berufliche Position zurück.
     *
     * @return berufliche Position
     */
    public String getRole() {
        return role;
    }

    /**
     * Setzt die berufliche Position.
     *
     * @param role neue berufliche Position
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Liefert den Arbeitsort zurück.
     *
     * @return Arbeitsort
     */
    public String getLocation() {
        return location;
    }

    /**
     * Setzt den Arbeitsort.
     *
     * @param location neuer Arbeitsort
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Liefert das Eintrittsdatum zurück.
     *
     * @return Eintrittsdatum
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Setzt das Eintrittsdatum.
     *
     * @param startDate neues Eintrittsdatum
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Liefert den Beschäftigungsstatus zurück.
     *
     * @return Beschäftigungsstatus
     */
    public String getStatus() {
        return status;
    }

    /**
     * Setzt den Beschäftigungsstatus.
     *
     * @param status neuer Beschäftigungsstatus
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
