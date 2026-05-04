package com.helpmi.dto.response;

import com.helpmi.domain.Organization;

import java.util.UUID;

public record OrganizationSummary(UUID id, String name) {
    public static OrganizationSummary from(Organization org) {
        return new OrganizationSummary(org.getId(), org.getName());
    }
}
