package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(min = 2, max = 10) @Pattern(regexp = "[A-Za-z0-9]+") String key,
        @Size(max = 10_000) String description
) {}
