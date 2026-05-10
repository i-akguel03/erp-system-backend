package com.erp.backend.controller;

import com.erp.backend.config.JwtUtil;
import com.erp.backend.config.TokenBlacklistService;
import com.erp.backend.dto.AuthRequest;
import com.erp.backend.dto.AuthResponse;
import com.erp.backend.dto.RegisterRequest;
import com.erp.backend.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (((UserDetailsServiceImpl) userDetailsService).userExists(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }
        ((UserDetailsServiceImpl) userDetailsService).createUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered");
    }

    /**
     * Logout: Invalidiert das aktuelle Access-Token sofort.
     * Das Token wird in die Blacklist eingetragen bis es natürlich abläuft.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @RequestBody(required = false) Map<String, String> body) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtUtil.extractJti(token);
                tokenBlacklistService.blacklist(jti, jwtUtil.extractExpiration(token));
            } catch (Exception e) {
                // Ungültiges Token beim Logout → trotzdem 200 zurückgeben
            }
        }

        if (body != null && body.containsKey("refreshToken")) {
            String refreshToken = body.get("refreshToken");
            try {
                if (jwtUtil.isRefreshToken(refreshToken)) {
                    String jti = jwtUtil.extractJti(refreshToken);
                    tokenBlacklistService.blacklist(jti, jwtUtil.extractExpiration(refreshToken));
                }
            } catch (Exception e) {
                // Ungültiges Refresh-Token beim Logout → ignorieren
            }
        }

        return ResponseEntity.ok(Map.of("message", "Erfolgreich abgemeldet"));
    }

    /**
     * Refresh: Erzeugt neue Tokens anhand eines gültigen Refresh-Tokens.
     * Das alte Refresh-Token wird dabei invalidiert (Rotation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "refreshToken fehlt"));
        }

        try {
            if (!jwtUtil.isTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh-Token ungültig oder abgelaufen"));
            }

            // Nur echte Refresh-Tokens akzeptieren
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Kein gültiges Refresh-Token"));
            }

            // Altes Refresh-Token sofort invalidieren (Token Rotation)
            String oldJti = jwtUtil.extractJti(refreshToken);
            if (tokenBlacklistService.isBlacklisted(oldJti)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh-Token wurde bereits verwendet"));
            }
            tokenBlacklistService.blacklist(oldJti, jwtUtil.extractExpiration(refreshToken));

            // Neue Tokens ausstellen
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            String newAccessToken  = jwtUtil.generateAccessToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            return ResponseEntity.ok(Map.of(
                    "accessToken",  newAccessToken,
                    "refreshToken", newRefreshToken,
                    "expiresIn",    jwtUtil.getAccessTokenValidity()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token-Refresh fehlgeschlagen"));
        }
    }
}
