package com.helpmi.dto.response;

import com.helpmi.domain.UserProject;

import java.util.UUID;

public record UserProjectRoleResponse(UUID projectId, String projectName, String role, boolean archived) {
    public static UserProjectRoleResponse from(UserProject up) {
        return new UserProjectRoleResponse(up.getProject().getId(), up.getProject().getName(), up.getRole(), up.getProject().isArchived());
    }
}
