package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String key,
        String description,
        int ticketSequence,
        long ticketCount,
        LocalDateTime createdAt,
        boolean canAssign,
        List<String> organizations,
        String userRole,
        boolean archived
) {}
