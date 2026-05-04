package com.helpmi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketHistoryResponse(
        UUID id,
        String field,
        String oldValue,
        String newValue,
        UserSummary changedBy,
        LocalDateTime changedAt
) {}
