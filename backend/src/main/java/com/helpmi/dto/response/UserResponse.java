package com.helpmi.dto.response;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, UserRole role,
                           UUID organizationId, String organizationName,
                           List<UserProjectRoleResponse> projectRoles, boolean active, LocalDateTime createdAt,
                           String theme) {
    public static UserResponse from(User user) {
        UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;
        List<UserProjectRoleResponse> projectRoles = user.getUserProjects().stream()
                .map(UserProjectRoleResponse::from)
                .sorted(Comparator.comparing(UserProjectRoleResponse::projectName))
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), orgId, orgName, projectRoles, user.isActive(), user.getCreatedAt(),
                user.getTheme());
    }
}
