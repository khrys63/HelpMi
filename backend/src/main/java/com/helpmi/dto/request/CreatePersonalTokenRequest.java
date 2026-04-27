package com.helpmi.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreatePersonalTokenRequest(
        @NotBlank @Size(max = 255) String name,
        @Future LocalDateTime expiresAt
) {}
