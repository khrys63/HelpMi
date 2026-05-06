package com.helpmi.controller;

import com.helpmi.dto.request.UpdateUserProjectsRequest;
import com.helpmi.dto.request.UpdateUserRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.getAllUsersForAdmin();
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) { // A5-M3
        return userService.updateUser(id, req);
    }

    @PostMapping("/{id}/organizations/{orgId}")
    public UserResponse addOrganization(@PathVariable UUID id, @PathVariable UUID orgId) {
        return userService.addOrganization(id, orgId);
    }

    @DeleteMapping("/{id}/organizations/{orgId}")
    public UserResponse removeOrganization(@PathVariable UUID id, @PathVariable UUID orgId) {
        return userService.removeOrganization(id, orgId);
    }

    @PutMapping("/{id}/projects")
    public UserResponse updateProjects(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateUserProjectsRequest req) { // A5-M3
        return userService.updateUserProjects(id, req);
    }
}
