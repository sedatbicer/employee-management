package de.employee.management.authservice.config;

import de.employee.management.authservice.auth.entity.AppUser;
import de.employee.management.authservice.auth.repository.AppUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Konfigurationsklasse zur Initialisierung des Administratorkontos.
 *
 * Prüft beim Start des Auth-Service, ob ein Administrator mit der
 * konfigurierten E-Mail-Adresse existiert. Falls nicht, wird ein neues
 * Administratorkonto mit gehashtem Passwort angelegt.
 *
 * Bereits vorhandene Administratorkonten werden nicht verändert.
 */
@Configuration
public class AdminBootstrap {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrap.class);

    /**
     * Erstellt bei Bedarf das initiale Administratorkonto.
     *
     * Die Zugangsdaten werden über die Anwendungskonfiguration
     * bereitgestellt. Das Passwort wird vor der Speicherung gehasht.
     *
     * @param userRepository Repository für Benutzerkonten
     * @param passwordEncoder Encoder für die Passwortverschlüsselung
     * @param adminEmail konfigurierte E-Mail-Adresse des Administrators
     * @param adminPassword konfiguriertes Passwort des Administrators
     * @return Initialisierungsroutine für den Anwendungsstart
     */
    @Bean
    CommandLineRunner createInitialAdmin(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {

        return args -> {

            if (adminEmail == null || adminEmail.isBlank()
                    || adminPassword == null || adminPassword.isBlank()) {

                throw new IllegalStateException(
                        "Administrator-E-Mail und Passwort müssen konfiguriert sein."
                );
            }

            String normalizedEmail = adminEmail.trim();

            if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {

                AppUser admin = new AppUser();
                admin.setEmail(normalizedEmail);
                admin.setPasswordHash(
                        passwordEncoder.encode(adminPassword)
                );
                admin.setRole("ADMIN");
                admin.setEnabled(true);

                userRepository.save(admin);

                log.info("Initiales Administratorkonto wurde angelegt.");

            } else {
                log.info("Administratorkonto ist bereits vorhanden.");
            }
        };
    }
}
