package com.helpmi.dto.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTicketRequest(
        @Size(max = 500) String title,
        String description,
        String priority,
        String type,
        UUID assigneeId
) {}
