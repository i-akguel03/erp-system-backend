package com.erp.backend.service;

import com.erp.backend.domain.Notification;
import com.erp.backend.domain.Notification.NotificationSeverity;
import com.erp.backend.domain.Notification.NotificationType;
import com.erp.backend.dto.NotificationDto;
import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationEmailService emailService;

    public NotificationService(NotificationRepository repository, NotificationEmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    // ===== Lesen =====

    @Transactional(readOnly = true)
    public Page<NotificationDto> getForCurrentUser(Pageable pageable) {
        String username = currentUsername();
        Page<Notification> page = isAdmin() ?
                repository.findForAdmin(username, pageable) :
                repository.findForUser(username, pageable);
        return page.map(NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long countUnreadForCurrentUser() {
        String username = currentUsername();
        return isAdmin() ?
                repository.countUnreadForAdmin(username) :
                repository.countUnreadForUser(username);
    }

    // ===== Schreiben =====

    public NotificationDto markAsRead(UUID id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification nicht gefunden: " + id));
        n.setRead(true);
        return NotificationDto.from(repository.save(n));
    }

    public void markAllReadForCurrentUser() {
        String username = currentUsername();
        if (isAdmin()) {
            repository.markAllReadForAdmin(username);
        } else {
            repository.markAllReadForUser(username);
        }
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Notification nicht gefunden: " + id);
        }
        repository.deleteById(id);
    }

    // ===== Erstellen (intern) =====

    public Notification create(NotificationType type, NotificationSeverity severity,
                               String title, String message,
                               String entityType, String entityId) {
        // Deduplizierung: gleiche Notification für dieselbe Entität nicht mehrmals am gleichen Tag
        if (entityId != null) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            if (repository.existsRecentForEntity(type, entityId, startOfDay)) {
                logger.debug("Notification für {} {} bereits heute erstellt – übersprungen", type, entityId);
                return null;
            }
        }

        Notification n = new Notification(type, severity, title, message, entityType, entityId);
        Notification saved = repository.save(n);
        logger.info("Notification erstellt: [{}] {}", severity, title);

        emailService.sendIfEnabled(saved);
        return saved;
    }

    // ===== Cleanup (wird per Scheduler aufgerufen) =====

    public int deleteOldNotifications(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return repository.deleteOldReadNotifications(cutoff);
    }

    // ===== Hilfsmethoden =====

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
