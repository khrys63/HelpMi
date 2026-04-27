package com.helpmi.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdateUserProjectsRequest(List<ProjectRoleEntry> entries) {
    public record ProjectRoleEntry(UUID projectId, String role) {}
}
