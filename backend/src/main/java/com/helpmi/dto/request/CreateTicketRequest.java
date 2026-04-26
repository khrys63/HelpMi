package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank @Size(max = 500) String title,
        String description,
        String priority,
        String type,
        UUID assigneeId,
        LocalDate dueDate
) {}
