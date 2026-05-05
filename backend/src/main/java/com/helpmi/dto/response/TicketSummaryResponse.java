package com.helpmi.dto.response;

import com.helpmi.domain.Ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        String reference,
        String title,
        String status,
        String priority,
        String type,
        LocalDate dueDate,
        UUID projectId,
        String projectKey,
        String projectName,
        UserSummary assignee,
        LocalDateTime updatedAt
) {
    public static TicketSummaryResponse from(Ticket t) {
        return new TicketSummaryResponse(
                t.getId(),
                t.getReference(),
                t.getTitle(),
                t.getStatus(),
                t.getPriority(),
                t.getType(),
                t.getDueDate(),
                t.getProject().getId(),
                t.getProject().getKey(),
                t.getProject().getName(),
                UserSummary.from(t.getAssignee()),
                t.getUpdatedAt()
        );
    }
}
