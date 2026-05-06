package com.helpmi.dto.response;

import java.util.List;
import java.util.UUID;

public record ManagerProjectTrackingResponse(
        UUID projectId,
        String key,
        String name,
        List<AssigneeTicketResponse> assignees,
        TicketCountResponse unassignedCounts,
        List<TicketSummaryResponse> unassignedTickets
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID projectId;
        private String key;
        private String name;
        private List<AssigneeTicketResponse> assignees;
        private TicketCountResponse unassignedCounts;
        private List<TicketSummaryResponse> unassignedTickets;

        public Builder projectId(UUID projectId) { this.projectId = projectId; return this; }
        public Builder key(String key) { this.key = key; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder assignees(List<AssigneeTicketResponse> assignees) { this.assignees = assignees; return this; }
        public Builder unassignedCounts(TicketCountResponse unassignedCounts) { this.unassignedCounts = unassignedCounts; return this; }
        public Builder unassignedTickets(List<TicketSummaryResponse> unassignedTickets) { this.unassignedTickets = unassignedTickets; return this; }

        public ManagerProjectTrackingResponse build() {
            return new ManagerProjectTrackingResponse(projectId, key, name, assignees, unassignedCounts, unassignedTickets);
        }
    }
}
