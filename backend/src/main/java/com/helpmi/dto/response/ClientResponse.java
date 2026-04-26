package com.helpmi.dto.response;

import com.helpmi.domain.Client;

import java.util.UUID;

public record ClientResponse(UUID id, String name, String contactEmail, boolean active) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(c.getId(), c.getName(), c.getContactEmail(), c.isActive());
    }
}
