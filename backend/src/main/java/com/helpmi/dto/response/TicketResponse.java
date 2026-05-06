package com.helpmi.dto.response;

import com.helpmi.domain.enums.ResolutionType;
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
        ResolutionType resolutionType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt
) {
    public static TicketResponse from(com.helpmi.domain.Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getType(),
                ticket.getDueDate(),
                ticket.getProject().getId(),
                ticket.getProject().getKey(),
                ticket.getReporter() != null ? UserSummary.from(ticket.getReporter()) : null,
                ticket.getAssignee() != null ? UserSummary.from(ticket.getAssignee()) : null,
                ticket.getResolutionType(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt()
        );
    }
}
