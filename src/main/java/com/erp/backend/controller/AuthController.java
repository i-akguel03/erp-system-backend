package com.erp.backend.controller;

import com.erp.backend.config.JwtUtil;
import com.erp.backend.dto.AuthRequest;
import com.erp.backend.dto.AuthResponse;
import com.erp.backend.dto.RefreshRequest;
import com.erp.backend.dto.RegisterRequest;
import com.erp.backend.service.UserDetailsServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller für Authentifizierung (Login, Registration, Refresh).
 * Alle Endpunkte liegen unter /auth/**
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin // Erlaubt CORS für diese Endpoints
public class AuthController {

    private final AuthenticationManager authenticationManager; // kümmert sich um Login
    private final UserDetailsService userDetailsService;       // lädt Benutzer (impl. = UserDetailsServiceImpl)
    private final JwtUtil jwtUtil;                             // erzeugt und prüft Tokens

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Login-Endpoint.
     * Erwartet: JSON mit { "username": "...", "password": "..." }
     * Antwort: AccessToken + RefreshToken
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        // Authentifizierung über AuthenticationManager
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Principal (UserDetails) aus der Authentication holen
        UserDetails user = (UserDetails) auth.getPrincipal();

        // Tokens generieren
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Antwort zurückgeben (DTO)
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    /**
     * Registrierung neuer User.
     * Erwartet: JSON mit { "username": "...", "password": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // prüfen, ob User schon existiert
        if (((UserDetailsServiceImpl) userDetailsService)
                .userExists(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }

        // neuen User anlegen
        ((UserDetailsServiceImpl) userDetailsService).createUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
    }

    /**
     * Refresh-Endpoint: erzeugt ein neues Access-Token anhand des Refresh-Tokens.
     * Erwartet: JSON mit { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        try {
            // prüfen, ob Refresh-Token gültig ist
            if (jwtUtil.isTokenValid(refreshToken)) {
                String username = jwtUtil.extractUsername(refreshToken);

                // User laden
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Neue Tokens generieren
                String newAccessToken = jwtUtil.generateAccessToken(userDetails);
                String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

                // Antwort-Map bauen
                Map<String, Object> response = new HashMap<>();
                response.put("accessToken", newAccessToken);
                response.put("refreshToken", newRefreshToken);
                response.put("expiresIn", jwtUtil.getAccessTokenValidity());

                return ResponseEntity.ok(response);
            } else {
                // Refresh-Token ungültig
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }
        } catch (Exception e) {
            // Fehler beim Refresh
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }
}
