package com.helpmi.controller;

import com.helpmi.dto.request.CreateOrganizationRequest;
import com.helpmi.dto.request.UpdateOrganizationRequest;
import com.helpmi.dto.response.OrganizationResponse;
import com.helpmi.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public List<OrganizationResponse> list() {
        return organizationService.listAll();
    }

    @GetMapping("/{id}")
    public OrganizationResponse get(@PathVariable UUID id) {
        return organizationService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest req) {
        return organizationService.create(req);
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest req) {
        return organizationService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        organizationService.delete(id);
    }

    @PostMapping("/{orgId}/projects/{projectId}")
    public OrganizationResponse addProject(@PathVariable UUID orgId, @PathVariable UUID projectId) {
        return organizationService.addProject(orgId, projectId);
    }

    @DeleteMapping("/{orgId}/projects/{projectId}")
    public OrganizationResponse removeProject(@PathVariable UUID orgId, @PathVariable UUID projectId) {
        return organizationService.removeProject(orgId, projectId);
    }

    @PostMapping("/{orgId}/users/{userId}")
    public OrganizationResponse addUser(@PathVariable UUID orgId, @PathVariable UUID userId) {
        return organizationService.addUserToOrganization(orgId, userId);
    }

    @DeleteMapping("/{orgId}/users/{userId}")
    public OrganizationResponse removeUser(@PathVariable UUID orgId, @PathVariable UUID userId) {
        return organizationService.removeUserFromOrganization(orgId, userId);
    }
}
