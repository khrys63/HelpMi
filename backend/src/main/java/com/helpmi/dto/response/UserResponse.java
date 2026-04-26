package com.helpmi.dto.response;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, UserRole role,
                           UUID organizationId, String organizationName, boolean active, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
        UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), orgId, orgName, user.isActive(), user.getCreatedAt());
    }
}
