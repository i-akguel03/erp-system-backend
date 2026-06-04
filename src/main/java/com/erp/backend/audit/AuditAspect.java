package com.erp.backend.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;
    private final AuditSettings auditSettings;

    public AuditAspect(AuditService auditService, AuditSettings auditSettings) {
        this.auditService = auditService;
        this.auditSettings = auditSettings;
    }

    @Pointcut("execution(* com.erp.backend.service.*Service.create*(..)) || " +
              "execution(* com.erp.backend.service.*Service.update*(..)) || " +
              "execution(* com.erp.backend.service.*Service.delete*(..)) || " +
              "execution(* com.erp.backend.service.*Service.save*(..))   || " +
              "execution(* com.erp.backend.service.*Service.add*(..))    || " +
              "execution(* com.erp.backend.service.*Service.remove*(..))")
    public void auditableServiceMethods() {}

    @Around("auditableServiceMethods()")
    public Object auditServiceCall(ProceedingJoinPoint pjp) throws Throwable {

        String methodName   = pjp.getSignature().getName();
        String serviceName  = pjp.getTarget().getClass().getSimpleName();
        String entityType   = resolveEntityType(serviceName);
        AuditLog.AuditAction action = resolveAction(methodName);
        Object[] args = pjp.getArgs();

        if (auditSettings.isExcluded(entityType)) {
            return pjp.proceed();
        }

        // Alten Zustand VOR der Änderung laden (nur bei Update und Delete sinnvoll)
        String oldValues = null;
        if (action == AuditLog.AuditAction.UPDATE || action == AuditLog.AuditAction.DELETE) {
            try {
                Object rawId = AuditService.extractRawEntityId(args);
                if (rawId != null) {
                    oldValues = auditService.loadOldValue(entityType, rawId);
                }
            } catch (Exception e) {
                log.debug("Could not load old value before {}.{}: {}", serviceName, methodName, e.getMessage());
            }
        }

        // Methode ausführen
        Object result = pjp.proceed();

        // Audit-Eintrag speichern
        try {
            String entityId = AuditService.extractEntityId(result, args);
            auditService.log(entityType, entityId, action, methodName, oldValues, result);
        } catch (Exception e) {
            log.warn("Audit logging failed for {}.{}: {}", serviceName, methodName, e.getMessage());
        }

        return result;
    }

    private String resolveEntityType(String serviceName) {
        return serviceName
                .replace("ServiceImpl", "")
                .replace("Service", "");
    }

    private AuditLog.AuditAction resolveAction(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.startsWith("create") || lower.startsWith("add")) {
            return AuditLog.AuditAction.CREATE;
        } else if (lower.startsWith("delete") || lower.startsWith("remove")) {
            return AuditLog.AuditAction.DELETE;
        } else {
            return AuditLog.AuditAction.UPDATE;
        }
    }
}
