package com.helpmi.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateThemeRequest(
        @Pattern(regexp = "light|dark") String theme
) {}
