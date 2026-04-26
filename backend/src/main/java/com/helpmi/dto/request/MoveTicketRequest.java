package com.helpmi.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MoveTicketRequest(@NotNull UUID targetProjectId) {}
