package com.helpmi.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255) String name,
        @Size(max = 10_000) String description
) {}
