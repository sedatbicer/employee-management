
package de.employee.management.authservice.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Zentrale Sicherheitskonfiguration des Auth-Service.
 *
 * Definiert die Passwortverarbeitung, die Zugriffsregeln für
 * HTTP-Anfragen, die Sitzungsverwaltung und die CORS-Richtlinien.
 *
 * Die Anwendung verwendet eine zustandslose Authentifizierung
 * mit JWT. Der Login-Endpunkt ist öffentlich erreichbar,
 * während geschützte Endpunkte eine gültige Authentifizierung
 * erfordern.
 *
 * Die CORS-Konfiguration wird über die Anwendungseigenschaft
 * app.cors.allowed-origins gesteuert. Dadurch kann dieselbe
 * Konfiguration sowohl mit Docker Compose als auch mit Kubernetes
 * verwendet werden.
 */
@Configuration
public class SecurityConfig {

    /**
     * Stellt den Passwort-Encoder für die sichere Speicherung
     * und Überprüfung von Benutzerpasswörtern bereit.
     *
     * @return BCrypt-basierter Passwort-Encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Konfiguriert die Sicherheitsregeln für eingehende HTTP-Anfragen.
     *
     * Der Login-Endpunkt ist öffentlich erreichbar. Alle weiteren
     * Endpunkte erfordern eine Authentifizierung.
     *
     * Die CORS-Verarbeitung verwendet die zentrale
     * CorsConfigurationSource und berücksichtigt dadurch die
     * konfigurierte Origin-Liste der jeweiligen Umgebung.
     *
     * @param http HTTP-Sicherheitskonfiguration von Spring Security
     * @return konfigurierte Security-Filterkette
     * @throws Exception bei Fehlern während der Sicherheitskonfiguration
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // CSRF ist deaktiviert, da die Anwendung zustandslos
            // mit JWT im Authorization-Header arbeitet.
            .csrf(csrf -> csrf.disable())

            // Aktiviert CORS mit der zentralen CORS-Konfiguration.
            .cors(Customizer.withDefaults())

            // Es werden keine serverseitigen HTTP-Sessions verwendet.
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Klassische Formular- und HTTP-Basic-Authentifizierung
            // werden nicht verwendet.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Der Login ist öffentlich; Health-Endpunkte sind ebenfalls
            // öffentlich erreichbar. Andere Endpunkte bleiben geschützt.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Definiert die erlaubten Ursprünge und HTTP-Header für CORS.
     *
     * Die erlaubten Ursprünge werden über die Anwendungseigenschaft
     * app.cors.allowed-origins bereitgestellt.
     *
     * Beispiel:
     * app.cors.allowed-origins=http://localhost:4200
     *
     * Mehrere Origins können kommasepariert angegeben werden.
     *
     * Wenn keine Origins konfiguriert sind, wird keine
     * Cross-Origin-Freigabe erteilt. Same-Origin-Anfragen,
     * beispielsweise über den Nginx-Reverse-Proxy in Kubernetes,
     * funktionieren weiterhin.
     *
     * @param allowedOrigins kommaseparierte Liste erlaubter Ursprünge
     * @return zentrale CORS-Konfiguration
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {

        // Konfigurierten String in eine bereinigte Origin-Liste umwandeln.
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();

        // Ohne konfigurierte Origins wird keine CORS-Regel angewendet.
        if (origins.isEmpty()) {
            return request -> null;
        }

        CorsConfiguration configuration = new CorsConfiguration();

        // Ausschließlich die explizit konfigurierten Origins erlauben.
        configuration.setAllowedOrigins(origins);

        // Erlaubte HTTP-Methoden für Cross-Origin-Anfragen.
        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        // Erlaubte Request-Header, insbesondere für JWT.
        configuration.setAllowedHeaders(
            List.of("Authorization", "Content-Type")
        );

        // Es werden keine browserseitigen Cookie-Credentials benötigt.
        configuration.setAllowCredentials(false);

        // Preflight-Ergebnisse dürfen eine Stunde zwischengespeichert werden.
        configuration.setMaxAge(3600L);

        // CORS-Konfiguration für alle API-Pfade registrieren.
        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
