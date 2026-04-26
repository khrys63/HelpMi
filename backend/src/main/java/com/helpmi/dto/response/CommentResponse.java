package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID ticketId,
        UserSummary author,
        String body,
        boolean edited,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
