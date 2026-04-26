package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String key,
        String description,
        int ticketSequence,
        long ticketCount,
        LocalDateTime createdAt
) {}
