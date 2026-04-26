package com.helpmi.dto.response;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;

import java.util.UUID;

public record UserSummary(UUID id, String email, String firstName, String lastName, UserRole role) {
    public static UserSummary from(User user) {
        if (user == null) return null;
        return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
    }
}
