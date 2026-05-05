package com.helpmi.controller;

import com.helpmi.dto.request.UpdateLocaleRequest;
import com.helpmi.dto.request.UpdateNotificationPrefsRequest;
import com.helpmi.dto.request.UpdateThemeRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/me/theme")
    public UserResponse updateTheme(@Valid @RequestBody UpdateThemeRequest req) {
        return userService.updateTheme(req);
    }

    @PatchMapping("/me/locale")
    public UserResponse updateLocale(@Valid @RequestBody UpdateLocaleRequest req) {
        return userService.updateLocale(req);
    }

    @PatchMapping("/me/notifications")
    public UserResponse updateNotificationPrefs(@Valid @RequestBody UpdateNotificationPrefsRequest req) {
        return userService.updateNotificationPrefs(req);
    }
}
