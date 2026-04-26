package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID ticketId,
        String fileName,
        String contentType,
        long size,
        UserSummary uploadedBy,
        LocalDateTime uploadedAt,
        String downloadUrl
) {}
