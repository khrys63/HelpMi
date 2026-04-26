package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LabelRequest(
        @NotBlank String name,
        String color
) {}
