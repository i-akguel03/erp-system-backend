package com.erp.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Hilfsklasse für JWT-Operationen:
 * - Tokens erstellen (Access + Refresh)
 * - Username extrahieren
 * - Token auf Gültigkeit prüfen
 */
@Component
public class JwtUtil {

    // Secret-Key aus application.properties laden (oder Standardwert, falls nicht gesetzt)
    @Value("${jwt.secret:meinSehrSicheresGeheimnisWelchesLangGenugIstFuerHMAC256}")
    private String jwtSecret;

    // Ablaufzeit für Access-Token (Standard: 15 Minuten)
    @Value("${jwt.access-token.expiration:900000}")  // 900.000 ms = 15 Minuten
    private long ACCESS_TOKEN_VALIDITY;

    // Ablaufzeit für Refresh-Token (Standard: 7 Tage)
    @Value("${jwt.refresh-token.expiration:604800000}") // 7 * 24 * 60 * 60 * 1000
    private long REFRESH_TOKEN_VALIDITY;

    /**
     * Wandelt den Secret-String in einen Key um,
     * der für die Signierung und Validierung von JWTs genutzt wird.
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes); // generiert HMAC-SHA256 Key
    }

    /**
     * Erstellt ein Access-Token mit kurzer Laufzeit (z. B. 15 Minuten).
     * Enthält Username (subject) und Rollen.
     */
    public String generateAccessToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())                  // Subject = Username
                .claim("roles", user.getAuthorities())           // Rollen als Claim einbetten
                .setIssuedAt(new Date())                         // Ausstellungszeit
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY)) // Ablaufzeit
                .signWith(getSigningKey())                       // Signatur mit Secret-Key
                .compact();                                      // kompaktes JWT-Format
    }

    /**
     * Erstellt ein Refresh-Token mit längerer Laufzeit (z. B. 7 Tage).
     * Enthält nur den Username.
     */
    public String generateRefreshToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrahiert den Username aus einem JWT.
     * Falls das Token ungültig ist, wird eine Exception geworfen.
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())   // Key für Validierung setzen
                    .build()
                    .parseClaimsJws(token)            // Token parsen
                    .getBody()
                    .getSubject();                    // Subject = Username
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * Prüft, ob ein Token gültig ist UND zum angegebenen User passt.
     */
    public boolean isTokenValid(String token, UserDetails user) {
        try {
            final String username = extractUsername(token);
            return username.equals(user.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Prüft, ob das Token bereits abgelaufen ist.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date()); // true = abgelaufen
        } catch (JwtException e) {
            return true; // Bei Fehlern sicherheitshalber als "abgelaufen" behandeln
        }
    }

    // Getter für Laufzeiten (falls von außen gebraucht)
    public long getAccessTokenValidity() {
        return ACCESS_TOKEN_VALIDITY;
    }

    public long getRefreshTokenValidity() {
        return REFRESH_TOKEN_VALIDITY;
    }

    /**
     * Validierung ohne UserDetails.
     * -> nur prüfen, ob das Token gültig signiert ist und nicht abgelaufen.
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }
}
