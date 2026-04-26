package com.helpmi.dto.response;

import com.helpmi.domain.Ticket;

import java.util.UUID;

public record TicketSummary(UUID id, String reference, String title, String status, String projectKey, UUID projectId) {
    public static TicketSummary from(Ticket t) {
        return new TicketSummary(t.getId(), t.getReference(), t.getTitle(), t.getStatus(),
                t.getProject().getKey(), t.getProject().getId());
    }
}
