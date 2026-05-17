package com.erp.backend.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_target", columnList = "target_username"),
        @Index(name = "idx_notification_read", columnList = "read"),
        @Index(name = "idx_notification_created", columnList = "created_at")
})
public class Notification {

    public enum NotificationType {
        OVERDUE_OPEN_ITEM,
        EXPIRING_CONTRACT,
        EXPIRING_SUBSCRIPTION,
        INVOICE_BATCH_COMPLETED,
        INVOICE_BATCH_FAILED,
        NEW_CUSTOMER,
        NEW_CONTRACT,
        NEW_SUBSCRIPTION
    }

    public enum NotificationSeverity {
        INFO, WARNING, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    // null = sichtbar für alle Admins; gesetzt = nur für diesen User
    @Column(name = "target_username")
    private String targetUsername;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Referenz auf die betroffene Entität (optional)
    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Notification() {}

    public Notification(NotificationType type, NotificationSeverity severity,
                        String title, String message, String entityType, String entityId) {
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public UUID getId() { return id; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public NotificationSeverity getSeverity() { return severity; }
    public void setSeverity(NotificationSeverity severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
}
