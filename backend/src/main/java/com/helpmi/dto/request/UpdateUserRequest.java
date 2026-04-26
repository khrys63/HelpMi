package com.helpmi.dto.request;

import com.helpmi.domain.enums.UserRole;

public record UpdateUserRequest(UserRole role, Boolean active) {}
