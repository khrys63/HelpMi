package com.helpmi.dto.request;

import java.time.LocalDateTime;

public record CreatePersonalTokenRequest(String name, LocalDateTime expiresAt) {}
