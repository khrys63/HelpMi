package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigValueRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String label,
        @Size(max = 100) String labelEn,
        @Size(max = 100) String labelBg,
        @Size(max = 100) String inverseLabel,
        @Size(max = 100) String inverseLabelEn,
        @Size(max = 100) String inverseLabelBg,
        @Size(max = 50) String color,
        boolean active,
        int position
) {}
