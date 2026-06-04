package com.erp.backend.controller;

import com.erp.backend.audit.AuditSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AuditSettings auditSettings;
    private final String adminPassword;

    public AdminSettingsController(AuditSettings auditSettings,
                                   @Value("${app.init.delete-password}") String adminPassword) {
        this.auditSettings = auditSettings;
        this.adminPassword = adminPassword;
    }

    @GetMapping("/audit/excluded")
    public ResponseEntity<Set<String>> getExcludedEntityTypes() {
        return ResponseEntity.ok(auditSettings.getExcludedEntityTypes());
    }

    @PostMapping("/audit/excluded/{entityType}")
    public ResponseEntity<String> excludeEntityType(
            @PathVariable String entityType,
            @RequestParam String password) {
        if (!adminPassword.equals(password)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falsches Passwort");
        }
        auditSettings.exclude(entityType);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/audit/excluded/{entityType}")
    public ResponseEntity<String> includeEntityType(
            @PathVariable String entityType,
            @RequestParam String password) {
        if (!adminPassword.equals(password)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falsches Passwort");
        }
        auditSettings.include(entityType);
        return ResponseEntity.noContent().build();
    }
}
