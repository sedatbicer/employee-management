package de.employee.management.authservice.auth.repository;

import de.employee.management.authservice.auth.entity.AppUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository für den Datenbankzugriff auf Benutzerkonten.
 *
 * Erweitert JpaRepository um Such- und Existenzprüfungen
 * anhand der E-Mail-Adresse.
 */
public interface AppUserRepository
        extends JpaRepository<AppUser, Long> {

    /**
     * Sucht einen Benutzer anhand seiner E-Mail-Adresse,
     * unabhängig von Groß- und Kleinschreibung.
     *
     * @param email E-Mail-Adresse des Benutzers
     * @return Optional mit dem gefundenen Benutzer
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /**
     * Prüft, ob eine E-Mail-Adresse bereits vergeben ist.
     *
     * @param email zu prüfende E-Mail-Adresse
     * @return true, wenn ein Benutzer mit dieser E-Mail existiert
     */
    boolean existsByEmailIgnoreCase(String email);
}
