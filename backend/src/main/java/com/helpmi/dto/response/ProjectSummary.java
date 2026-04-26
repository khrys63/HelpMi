package com.helpmi.dto.response;

import com.helpmi.domain.Project;

import java.util.UUID;

public record ProjectSummary(UUID id, String name, String key) {
    public static ProjectSummary from(Project p) {
        return new ProjectSummary(p.getId(), p.getName(), p.getKey());
    }
}
