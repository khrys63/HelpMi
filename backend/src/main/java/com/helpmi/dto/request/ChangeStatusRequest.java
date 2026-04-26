package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatusRequest(@NotBlank String status) {}
