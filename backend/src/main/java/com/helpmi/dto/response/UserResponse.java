package com.helpmi.dto.response;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, UserRole role,
                           List<OrganizationSummary> organizations,
                           List<UserProjectRoleResponse> projectRoles, boolean active, LocalDateTime createdAt,
                           String theme, String locale,
                           boolean notifAssigned, boolean notifComment,
                           boolean notifStatusChanged, boolean notifWatcherAdded,
                           boolean notifTicketCreated) {
    public static UserResponse from(User user) {
        List<OrganizationSummary> organizations = user.getOrganizations().stream()
                .map(OrganizationSummary::from)
                .sorted(Comparator.comparing(OrganizationSummary::name))
                .toList();
        List<UserProjectRoleResponse> projectRoles = user.getUserProjects().stream()
                .map(UserProjectRoleResponse::from)
                .sorted(Comparator.comparing(UserProjectRoleResponse::projectName))
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), organizations, projectRoles, user.isActive(), user.getCreatedAt(),
                user.getTheme(), user.getLocale(),
                user.isNotifAssigned(), user.isNotifComment(),
                user.isNotifStatusChanged(), user.isNotifWatcherAdded(),
                user.isNotifTicketCreated());
    }
}
