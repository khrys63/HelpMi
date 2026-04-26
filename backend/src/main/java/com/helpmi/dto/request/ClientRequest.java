package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        String contactEmail,
        boolean active
) {}
