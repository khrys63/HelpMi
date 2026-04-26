package com.helpmi.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(@Size(max = 100) String name, Boolean active) {}
