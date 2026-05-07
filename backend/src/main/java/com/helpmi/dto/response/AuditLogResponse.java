package com.helpmi.dto.response;

import com.helpmi.domain.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        UUID actorId,
        String actorEmail,
        String targetType,
        String targetId,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction().name(),
                log.getActorId(),
                log.getActorEmail(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
