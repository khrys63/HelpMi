package com.helpmi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTicketLinkRequest(@NotNull UUID targetTicketId, @NotBlank String linkType) {}
