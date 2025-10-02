package com.erp.backend.service;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.UserEntity;
import com.erp.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Diese Klasse implementiert Spring Securitys UserDetailsService.
 * Sie wird von Spring Security benutzt, um beim Login den Benutzer
 * aus der Datenbank zu laden.
 *
 * Ablauf:
 * - User loggt sich mit Username/Passwort ein
 * - Spring ruft loadUserByUsername() auf
 * - Wir laden den User aus der DB und geben ihn als UserDetails zurück
 * - Spring prüft dann das Passwort (mit unserem PasswordEncoder)
 * - Rollen werden als Authorities gesetzt
 */
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;   // DB-Zugriff auf User
    private final PasswordEncoder passwordEncoder; // für Passwort-Hashing

    // Konstruktor-Injection
    public UserDetailsServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Diese Methode wird von Spring Security beim Login aufgerufen.
     * Erwartet: Username → liefert UserDetails zurück.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // User aus DB laden oder Fehler werfen, falls nicht gefunden
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Rollen des Users holen
        var roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            // Falls User keine Rollen hat → Default-Rolle setzen
            roles = new HashSet<>();
            roles.add(Role.ROLE_USER);
        }

        // Rollen in Authorities konvertieren (z. B. "ROLE_ADMIN")
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());

        // Rückgabe: Spring Security User-Objekt
        // Enthält Username, Passwort (Hash), Authorities (Rollen)
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    // Prüfen, ob ein User bereits existiert
    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    // Neuen User erstellen (hier Standard: Admin)
    public UserEntity createUser(String username, String password) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Passwort sicher speichern

        // Standardmäßig ADMIN-Rolle vergeben
        user.addRole(Role.ROLE_ADMIN);

        return userRepository.save(user);
    }

    // Alternative sichere Methode: Rolle als Parameter übergeben
    public UserEntity createUserSafe(String username, String password, Role role) {
        UserEntity user = new UserEntity(username, passwordEncoder.encode(password));
        user.addRole(role != null ? role : Role.ROLE_USER); // Fallback: ROLE_USER
        return userRepository.save(user);
    }
}
