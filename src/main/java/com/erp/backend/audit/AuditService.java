package com.erp.backend.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String DOMAIN_PACKAGE = "com.erp.backend.domain.";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Transactional
    public void log(String entityType, String entityId, AuditLog.AuditAction action,
                    String methodName, String oldValues, Object newValue) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setChangedBy(getCurrentUsername());
        entry.setMethodName(methodName);
        entry.setOldValues(oldValues);
        entry.setNewValues(shallowSerialize(newValue));
        auditLogRepository.save(entry);
    }

    /**
     * Lädt den aktuellen Zustand einer Entität aus der DB (vor der Änderung).
     * Wird in einer eigenen Read-Only-Transaktion ausgeführt, damit Lazy-Loading funktioniert.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String loadOldValue(String entityType, Object entityId) {
        if (entityId == null) return null;
        try {
            Class<?> entityClass = Class.forName(DOMAIN_PACKAGE + entityType);
            Object entity = entityManager.find(entityClass, entityId);
            if (entity == null) return null;
            return shallowSerialize(entity);
        } catch (ClassNotFoundException e) {
            log.debug("No domain class found for entity type: {}", entityType);
            return null;
        } catch (Exception e) {
            log.debug("Could not load old value for {}/{}: {}", entityType, entityId, e.getMessage());
            return null;
        }
    }

    /**
     * Serialisiert nur primitive Wertfelder (String, UUID, Number, Enum, Boolean).
     * JPA-Relationen (@ManyToOne, @OneToMany usw.) werden bewusst übersprungen,
     * um Lazy-Loading-Fehler und Endlosrekursion zu vermeiden.
     */
    String shallowSerialize(Object entity) {
        if (entity == null) return null;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            Class<?> clazz = entity.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (isTransient(field)) continue;
                    field.setAccessible(true);
                    try {
                        if (isToOneRelation(field)) {
                            // Nur die ID der referenzierten Entität speichern, kein EAGER-Loading
                            Object related = field.get(entity);
                            if (related != null) {
                                try {
                                    Object relatedId = related.getClass().getMethod("getId").invoke(related);
                                    data.put(field.getName() + "Id", relatedId);
                                } catch (Exception ignored) {}
                            } else {
                                data.put(field.getName() + "Id", null);
                            }
                        } else if (!isToManyRelation(field)) {
                            data.put(field.getName(), field.get(entity));
                        }
                        // @OneToMany / @ManyToMany / @ElementCollection werden weiterhin übersprungen
                    } catch (Exception ignored) {}
                }
                clazz = clazz.getSuperclass();
            }
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return entity.toString();
        }
    }

    private boolean isToOneRelation(Field field) {
        return field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToOne.class);
    }

    private boolean isToManyRelation(Field field) {
        return field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToMany.class)
                || field.isAnnotationPresent(ElementCollection.class);
    }

    private boolean isJpaRelation(Field field) {
        return isToOneRelation(field) || isToManyRelation(field);
    }

    private boolean isTransient(Field field) {
        return field.isAnnotationPresent(Transient.class)
                || java.lang.reflect.Modifier.isTransient(field.getModifiers())
                || java.lang.reflect.Modifier.isStatic(field.getModifiers());
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    /**
     * Extrahiert die ID als String (für den AuditLog-Eintrag).
     */
    public static String extractEntityId(Object returnValue, Object[] args) {
        if (returnValue != null) {
            try {
                Object idVal = returnValue.getClass().getMethod("getId").invoke(returnValue);
                if (idVal != null) return idVal.toString();
            } catch (Exception ignored) {}
        }
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }

    /**
     * Extrahiert die rohe ID (UUID/Long/…) aus den Methodenargumenten für EntityManager.find().
     * Gibt null zurück, wenn keine auflösbare ID gefunden wird.
     */
    public static Object extractRawEntityId(Object[] args) {
        if (args == null || args.length == 0) return null;
        Object first = args[0];
        if (first == null) return null;
        // Direkt eine ID (UUID, Long, Integer, String)
        if (first instanceof UUID || first instanceof Long
                || first instanceof Integer || first instanceof String) {
            return first;
        }
        // Eine Entität mit getId()
        try {
            return first.getClass().getMethod("getId").invoke(first);
        } catch (Exception ignored) {}
        return null;
    }
}
