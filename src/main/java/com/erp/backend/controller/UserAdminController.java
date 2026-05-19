package com.erp.backend.controller;

import com.erp.backend.dto.UserAdminDto;
import com.erp.backend.entity.Role;
import com.erp.backend.entity.UserEntity;
import com.erp.backend.service.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Benutzerverwaltung")
public class UserAdminController {

    private final UserDetailsServiceImpl userDetailsService;

    public UserAdminController(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Operation(summary = "Alle Benutzer abrufen")
    @GetMapping
    public ResponseEntity<List<UserAdminDto>> getAllUsers() {
        List<UserAdminDto> users = userDetailsService.getAllUsers().stream()
                .map(UserAdminDto::new)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Verfügbare Rollen abrufen")
    @GetMapping("/roles")
    public ResponseEntity<Role[]> getAvailableRoles() {
        return ResponseEntity.ok(Role.values());
    }

    @Operation(summary = "Rollen eines Benutzers aktualisieren")
    @PutMapping("/{username}/roles")
    public ResponseEntity<?> updateRoles(
            @PathVariable String username,
            @RequestBody Map<String, Set<Role>> body) {
        Set<Role> newRoles = body.get("roles");
        if (newRoles == null || newRoles.isEmpty()) {
            return ResponseEntity.badRequest().body("Mindestens eine Rolle erforderlich");
        }
        UserEntity updated = userDetailsService.updateRoles(username, newRoles);
        return ResponseEntity.ok(new UserAdminDto(updated));
    }

    @Operation(summary = "Neuen Benutzer anlegen")
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Username und Passwort erforderlich");
        }

        if (userDetailsService.userExists(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User existiert bereits");
        }

        UserEntity created = userDetailsService.createUser(username, password);

        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) body.get("roles");
        if (roleNames != null && !roleNames.isEmpty()) {
            Set<Role> roles = new java.util.HashSet<>();
            for (String roleName : roleNames) {
                try {
                    roles.add(Role.valueOf(roleName));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Unbekannte Rolle: " + roleName);
                }
            }
            created = userDetailsService.updateRoles(username, roles);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserAdminDto(created));
    }

    @Operation(summary = "Benutzer löschen")
    @DeleteMapping("/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        userDetailsService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
