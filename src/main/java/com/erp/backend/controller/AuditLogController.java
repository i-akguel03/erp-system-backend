package com.erp.backend.controller;

import com.erp.backend.audit.AuditLog;
import com.erp.backend.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @PageableDefault(size = 50, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByChangedAtDesc(pageable));
    }

    @GetMapping("/entity/{entityType}")
    public ResponseEntity<List<AuditLog>> getByEntityType(@PathVariable String entityType) {
        return ResponseEntity.ok(auditLogRepository.findByEntityTypeOrderByChangedAtDesc(entityType));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getByEntityTypeAndId(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(
                auditLogRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(entityType, entityId));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<AuditLog>> getByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditLogRepository.findByChangedByOrderByChangedAtDesc(username));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getByAction(@PathVariable AuditLog.AuditAction action) {
        return ResponseEntity.ok(auditLogRepository.findByActionOrderByChangedAtDesc(action));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AuditLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(auditLogRepository.findByChangedAtBetweenOrderByChangedAtDesc(from, to));
    }
}