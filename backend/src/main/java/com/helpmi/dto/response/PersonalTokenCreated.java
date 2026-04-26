package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PersonalTokenCreated(
        UUID id,
        String name,
        String plainToken,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {}
