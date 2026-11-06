
package de.employee.management.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import de.employee.management.security.JwtAuthenticationFilter;

/**
 * Zentrale Sicherheitskonfiguration für den Employee-Service.
 *
 * Definiert die HTTP-Sicherheitsregeln, die JWT-basierte
 * Authentifizierung und die CORS-Konfiguration.
 *
 * Die Anwendung verwendet eine zustandslose Authentifizierung.
 * Jeder geschützte Request muss deshalb seinen JWT im
 * Authorization-Header übermitteln.
 *
 * Die erlaubten CORS-Origins werden über die Anwendungskonfiguration
 * bereitgestellt. Dadurch kann dieselbe Sicherheitskonfiguration
 * sowohl mit Docker Compose als auch mit Kubernetes verwendet werden.
 *
 * Beim Kubernetes-Betrieb über den Nginx-Reverse-Proxy erfolgen
 * die API-Aufrufe über dieselbe Browser-Origin. Dafür ist keine
 * zusätzliche Cross-Origin-Freigabe erforderlich.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Injiziert den JWT-Filter zur Überprüfung eingehender Requests.
     *
     * @param jwtAuthenticationFilter Filter für die JWT-Authentifizierung
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Erstellt die zentrale Spring-Security-Filterkette.
     *
     * Alle regulären API-Anfragen benötigen eine gültige Authentifizierung.
     * Die rollenbasierte Autorisierung erfolgt zusätzlich auf Methodenebene.
     *
     * Die CORS-Verarbeitung wird über die zentrale
     * CorsConfigurationSource aktiviert.
     *
     * @param http HTTP-Sicherheitskonfiguration von Spring Security
     * @return konfigurierte Sicherheitsfilterkette
     * @throws Exception wenn die Sicherheitskonfiguration fehlschlägt
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // CSRF wird deaktiviert, da die API zustandslos mit JWT arbeitet.
            .csrf(csrf -> csrf.disable())

            // Aktiviert CORS und verwendet die unten definierte Konfiguration.
            .cors(Customizer.withDefaults())

            // Es werden keine serverseitigen HTTP-Sessions verwendet.
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Die API verwendet weder Formular-Login noch HTTP Basic.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // OPTIONS-Anfragen werden für CORS-Preflight freigegeben.
            // Der Login- und Health-Endpunkt sind öffentlich erreichbar.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )

            // JWT-Authentifizierung vor dem Standard-Authentifizierungsfilter.
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Definiert die erlaubten Ursprünge und HTTP-Header für CORS.
     *
     * Die erlaubten Ursprünge werden über die Anwendungseigenschaft
     * app.cors.allowed-origins bereitgestellt.
     *
     * Dadurch können für unterschiedliche Betriebsumgebungen
     * unterschiedliche Origins konfiguriert werden, ohne den
     * Anwendungscode ändern zu müssen.
     *
     * Beispiel:
     * app.cors.allowed-origins=http://localhost:4200
     *
     * Mehrere Origins können kommasepariert angegeben werden.
     *
     * Wenn keine Origins konfiguriert sind, wird keine
     * Cross-Origin-Freigabe erteilt. Same-Origin-Anfragen,
     * beispielsweise über den Nginx-Reverse-Proxy in Kubernetes,
     * werden dadurch nicht verhindert.
     *
     * @param allowedOrigins kommaseparierte Liste erlaubter Ursprünge
     * @return zentrale CORS-Konfiguration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
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
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
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
