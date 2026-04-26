package com.helpmi.dto.response;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, UserRole role, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole(), user.getCreatedAt());
    }
}
