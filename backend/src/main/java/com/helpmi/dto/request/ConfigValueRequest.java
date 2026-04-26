package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigValueRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String label,
        @Size(max = 50) String color,
        boolean active,
        int position
) {}
