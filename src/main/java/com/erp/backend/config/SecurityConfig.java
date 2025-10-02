package com.erp.backend.config;

import com.erp.backend.repository.UserRepository;
import com.erp.backend.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Zentrale Spring Security Konfiguration.
 * - Definiert Passwort-Verschlüsselung
 * - Setzt AuthenticationManager + UserDetailsService
 * - Bindet den JwtAuthFilter ein
 * - Definiert Security-Regeln für Endpoints
 * - Konfiguriert CORS für Frontend-Requests
 */
@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil; // Hilfsklasse für JWT-Operationen
    private final UserRepository userRepository; // Repository für User (aus DB)

    // Konstruktor-Injection
    public SecurityConfig(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // Bean für Passwortverschlüsselung
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Hashing mit BCrypt
    }

    // Bean für eigenen UserDetailsService (lädt User aus DB)
    @Bean
    public UserDetailsServiceImpl userDetailsService(PasswordEncoder passwordEncoder) {
        return new UserDetailsServiceImpl(userRepository, passwordEncoder);
    }

    // Bean für AuthenticationManager (wird beim Login verwendet)
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsServiceImpl userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // verbindet DB-User-Service
        provider.setPasswordEncoder(passwordEncoder);       // nutzt BCrypt für Passwortprüfung
        return new ProviderManager(provider); // AuthenticationManager mit Provider
    }

    // Bean für unseren JWT-Filter
    @Bean
    public JwtAuthFilter jwtAuthFilter(UserDetailsServiceImpl userDetailsService) {
        return new JwtAuthFilter(userDetailsService, jwtUtil);
    }

    /**
     * Zentrale Security-Definition:
     * - CSRF deaktiviert (nicht nötig bei REST + JWT)
     * - Endpoints freigeben/schützen
     * - Session-Management auf STATELESS (kein HttpSession, nur JWT)
     * - Unser JWT-Filter einbinden
     * - CORS konfigurieren (für Frontend)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                // CSRF deaktivieren, da JWT keinen Session-Schutz benötigt
                .csrf(csrf -> csrf.disable())

                // Zugriffskontrolle für Endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()           // Authentifizierung frei
                        .requestMatchers("/actuator/health").permitAll()   // Health-Check frei (z. B. für Frontend/Monitoring)
                        .requestMatchers("/api/**").authenticated()        // alle /api/** brauchen Login
                        .anyRequest().permitAll()                         // Rest erlauben
                )

                // Session-Management: Stateless (keine Session, immer JWT)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Unser JWT-Filter kommt vor dem Standard-Filter von Spring Security
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // CORS aktivieren mit Standard-Einstellungen (nimmt unsere Bean unten)
                .cors(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Definiert, welche Frontend-Domains auf unsere API zugreifen dürfen (CORS).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Erlaubte Ursprünge (Domains vom Frontend)
        configuration.setAllowedOrigins(List.of(
                "https://erp-system-frontend-tan.vercel.app",
                "http://localhost:4200",
                "https://erp-system-frontend-kw82ubi6v-akibos-projects.vercel.app"
        ));

        // Erlaubte HTTP-Methoden
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));

        // Erlaubte Header (z. B. Authorization, Content-Type)
        configuration.setAllowedHeaders(List.of("*"));

        // Cookies/Credentials erlauben (z. B. für Auth)
        configuration.setAllowCredentials(true);

        // Registriert die CORS-Konfiguration für alle Pfade
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
