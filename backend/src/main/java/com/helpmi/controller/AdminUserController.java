package com.helpmi.controller;

import com.helpmi.dto.request.AssignOrganizationRequest;
import com.helpmi.dto.request.UpdateUserRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.service.UserService;
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
    public UserResponse update(@PathVariable UUID id, @RequestBody UpdateUserRequest req) {
        return userService.updateUser(id, req);
    }

    @PutMapping("/{id}/organization")
    public UserResponse assignOrganization(@PathVariable UUID id,
                                           @RequestBody AssignOrganizationRequest req) {
        return userService.assignOrganization(id, req);
    }
}
