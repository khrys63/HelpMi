package com.helpmi.dto.response;

import java.util.List;

public record DashboardResponse(
        List<TicketSummaryResponse> myOpenTickets,
        List<TicketSummaryResponse> assignedToMe,
        List<TicketSummaryResponse> watchedTickets,
        List<TicketSummaryResponse> dueSoon,
        List<ProjectTicketStatsResponse> projectStats
) {}
