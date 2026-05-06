package com.helpmi.dto.request;

import com.helpmi.domain.enums.ResolutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
    @NotBlank String status,
    ResolutionType resolutionType,
    String comment
) {}
