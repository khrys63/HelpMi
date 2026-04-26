package com.helpmi.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String reference,
        String title,
        String status,
        String priority,
        String type,
        LocalDate dueDate,
        UUID projectId,
        String projectKey,
        UserSummary reporter,
        UserSummary assignee,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt
) {}
