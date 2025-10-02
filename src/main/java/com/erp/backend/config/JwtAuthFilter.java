package com.erp.backend.config;

// Importiert die benötigten Klassen
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter ist ein Filter, der bei jeder HTTP-Anfrage aufgerufen wird.
 * Er prüft, ob im Header ein gültiges JWT-Token vorhanden ist.
 * Falls ja, authentifiziert er den Benutzer in Spring Security.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // Hilfsklasse für Token-Operationen (z. B. extrahieren, validieren)
    private final UserDetailsService userDetailsService; // Zum Laden von User-Details aus der DB

    // Konstruktor-Injection: Spring gibt automatisch die Abhängigkeiten rein
    public JwtAuthFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Diese Methode wird bei jeder eingehenden HTTP-Request aufgerufen.
     * Sie prüft, ob ein JWT im "Authorization"-Header vorhanden und gültig ist.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Lese den Wert des Authorization-Headers (z. B. "Bearer abc123...")
        final String authHeader = request.getHeader("Authorization");

        // Wenn kein Header vorhanden ist oder er nicht mit "Bearer " anfängt -> weiter im Filter-Chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Request geht ungeprüft weiter
            return; // Methode beenden
        }

        // Schneidet "Bearer " ab, sodass nur das Token übrig bleibt
        final String token = authHeader.substring(7);

        // Extrahiert den Username aus dem Token (steht normalerweise im "sub"-Claim des JWT)
        final String username = jwtUtil.extractUsername(token);

        // Wenn ein Username da ist und noch niemand im SecurityContext authentifiziert ist
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Lade die UserDetails (z. B. Rollen, Passwort-Hash) aus der DB
            UserDetails user = userDetailsService.loadUserByUsername(username);

            // Prüfe, ob das Token wirklich gültig ist (z. B. Signatur + Ablaufdatum)
            if (jwtUtil.isTokenValid(token, user)) {
                // Erstellt ein Authentication-Objekt für Spring Security
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                // Setzt die Authentifizierung in den SecurityContext
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Gebe die Anfrage im Filter-Chain weiter, damit Controller/Endpoints aufgerufen werden können
        filterChain.doFilter(request, response);
    }
}
