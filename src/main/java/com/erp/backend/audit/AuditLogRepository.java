package com.erp.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);

    List<AuditLog> findByEntityTypeOrderByChangedAtDesc(String entityType);

    List<AuditLog> findByEntityIdOrderByChangedAtDesc(String entityId);

    List<AuditLog> findByChangedByOrderByChangedAtDesc(String changedBy);

    List<AuditLog> findByActionOrderByChangedAtDesc(AuditLog.AuditAction action);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, String entityId);

    List<AuditLog> findByChangedAtBetweenOrderByChangedAtDesc(LocalDateTime from, LocalDateTime to);
}