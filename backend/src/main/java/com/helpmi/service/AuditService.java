package com.helpmi.service;

import com.helpmi.domain.AuditLog;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.AuditAction;
import com.helpmi.repository.AuditLogRepository;
import com.helpmi.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    /** Log an action using the currently authenticated user as actor. */
    public void log(AuditAction action, String targetType, String targetId, String details) {
        try {
            User actor = tryGetCurrentUser();
            persist(action,
                    actor != null ? actor.getId() : null,
                    actor != null ? actor.getEmail() : null,
                    targetType, targetId, details);
        } catch (Exception e) {
            log.error("Audit log failed [{}]: {}", action, e.getMessage());
        }
    }

    /** Log an action with an explicit actor (e.g., during authentication before SecurityContext is set). */
    public void log(AuditAction action, UUID actorId, String actorEmail,
                    String targetType, String targetId, String details) {
        try {
            persist(action, actorId, actorEmail, targetType, targetId, details);
        } catch (Exception e) {
            log.error("Audit log failed [{}]: {}", action, e.getMessage());
        }
    }

    private void persist(AuditAction action, UUID actorId, String actorEmail,
                         String targetType, String targetId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(extractIp())
                .build());
    }

    private User tryGetCurrentUser() {
        try {
            return currentUserService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
