package de.employee.management.authservice.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

/**
 * JPA-Entity für Benutzerkonten des Auth-Service.
 *
 * Speichert die Zugangsdaten, die Rolle und den Aktivierungsstatus
 * eines Benutzers. Passwörter werden ausschließlich als Hash gespeichert.
 */
@Entity
@Table(
    name = "app_users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_app_users_email",
            columnNames = "email"
        )
    }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Standardkonstruktor für JPA.
     */
    public AppUser() {
    }

    /**
     * Initialisiert die Erstellungszeit und normalisiert die E-Mail-Adresse
     * vor dem erstmaligen Speichern.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        email = normalizeEmail(email);
    }

    /**
     * Normalisiert E-Mail-Adressen unabhängig von der System-Locale.
     *
     * @param email ursprüngliche E-Mail-Adresse
     * @return normalisierte E-Mail-Adresse oder null
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
