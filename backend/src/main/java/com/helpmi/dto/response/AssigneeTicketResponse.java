package com.helpmi.dto.response;

import java.util.List;
import java.util.UUID;

public record AssigneeTicketResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        TicketCountResponse counts,
        List<TicketSummaryResponse> tickets
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private TicketCountResponse counts;
        private List<TicketSummaryResponse> tickets;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder counts(TicketCountResponse counts) { this.counts = counts; return this; }
        public Builder tickets(List<TicketSummaryResponse> tickets) { this.tickets = tickets; return this; }

        public AssigneeTicketResponse build() {
            return new AssigneeTicketResponse(id, email, firstName, lastName, counts, tickets);
        }
    }
}
