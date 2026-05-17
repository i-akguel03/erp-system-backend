package com.erp.backend.dto;

import com.erp.backend.domain.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDto {

    private UUID id;
    private Notification.NotificationType type;
    private Notification.NotificationSeverity severity;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private String entityType;
    private String entityId;

    public static NotificationDto from(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.id = n.getId();
        dto.type = n.getType();
        dto.severity = n.getSeverity();
        dto.title = n.getTitle();
        dto.message = n.getMessage();
        dto.read = n.isRead();
        dto.createdAt = n.getCreatedAt();
        dto.entityType = n.getEntityType();
        dto.entityId = n.getEntityId();
        return dto;
    }

    public UUID getId() { return id; }
    public Notification.NotificationType getType() { return type; }
    public Notification.NotificationSeverity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
