package com.erp.backend.controller;

import com.erp.backend.audit.AuditSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AuditSettings auditSettings;

    public AdminSettingsController(AuditSettings auditSettings) {
        this.auditSettings = auditSettings;
    }

    @GetMapping("/audit/excluded")
    public ResponseEntity<Set<String>> getExcludedEntityTypes() {
        return ResponseEntity.ok(auditSettings.getExcludedEntityTypes());
    }

    @PostMapping("/audit/excluded/{entityType}")
    public ResponseEntity<Void> excludeEntityType(@PathVariable String entityType) {
        auditSettings.exclude(entityType);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/audit/excluded/{entityType}")
    public ResponseEntity<Void> includeEntityType(@PathVariable String entityType) {
        auditSettings.include(entityType);
        return ResponseEntity.noContent().build();
    }
}
