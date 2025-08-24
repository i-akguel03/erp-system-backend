package com.erp.backend.service;

import com.erp.backend.entity.Role;
import com.erp.backend.entity.UserEntity;
import com.erp.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.stream.Collectors;

public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDetailsServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // ✅ Null-Safe: Rollen prüfen
        var roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            // Falls keine Rollen vorhanden, Standard-Rolle setzen
            roles = new HashSet<>();
            roles.add(Role.ROLE_USER); // oder ROLE_ADMIN je nach Bedarf
        }

        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    // Prüfen, ob User existiert
    public boolean userExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    // ✅ VERBESSERT: User erstellen mit sicherer Rollen-Zuweisung
    public UserEntity createUser(String username, String password) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        // ✅ Sichere Methode: addRole() verwenden statt direkten Zugriff
        user.addRole(Role.ROLE_ADMIN); // oder ROLE_USER

        return userRepository.save(user);
    }

    // ✅ Alternative sichere Methode
    public UserEntity createUserSafe(String username, String password, Role role) {
        UserEntity user = new UserEntity(username, passwordEncoder.encode(password));
        user.addRole(role != null ? role : Role.ROLE_USER);
        return userRepository.save(user);
    }
}