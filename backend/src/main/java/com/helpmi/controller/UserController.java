package com.helpmi.controller;

import com.helpmi.dto.response.UserResponse;
import com.helpmi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.getActiveUsers();
    }

    @GetMapping("/me")
    public UserResponse getMe() {
        return userService.getCurrentUser();
    }
}
