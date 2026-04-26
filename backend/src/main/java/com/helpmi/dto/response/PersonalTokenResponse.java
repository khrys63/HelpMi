package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PersonalTokenResponse(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        LocalDateTime expiresAt
) {}
