package com.helpmi.dto.response;

import com.helpmi.domain.Organization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        boolean active,
        LocalDateTime createdAt,
        List<ProjectSummary> projects,
        List<UserResponse> users
) {
    public static OrganizationResponse from(Organization org, List<UserResponse> users) {
        List<ProjectSummary> projects = org.getProjects().stream()
                .map(ProjectSummary::from)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
        return new OrganizationResponse(org.getId(), org.getName(), org.isActive(), org.getCreatedAt(), projects, users);
    }
}
