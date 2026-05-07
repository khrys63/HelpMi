package com.helpmi.dto.response;

import java.util.UUID;

public record ProjectTicketStatsResponse(
        UUID projectId,
        String projectKey,
        String projectName,
        int open,
        int inProgress
) {}
