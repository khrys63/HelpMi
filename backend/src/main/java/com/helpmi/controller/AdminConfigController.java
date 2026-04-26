package com.helpmi.controller;

import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.ConfigValueRequest;
import com.helpmi.dto.response.ConfigValueResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.security.CurrentUserService;
import com.helpmi.service.AdminConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService adminConfigService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Map<String, List<ConfigValueResponse>> getAll() {
        return adminConfigService.getAllCategories();
    }

    @GetMapping("/{category}")
    public List<ConfigValueResponse> getCategory(@PathVariable String category) {
        return adminConfigService.getCategory(category.toUpperCase());
    }

    @PostMapping("/{category}")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfigValueResponse create(@PathVariable String category,
            @Valid @RequestBody ConfigValueRequest req) {
        requireAdmin();
        return adminConfigService.create(category.toUpperCase(), req);
    }

    @PutMapping("/{category}/{id}")
    public ConfigValueResponse update(@PathVariable String category, @PathVariable UUID id,
            @Valid @RequestBody ConfigValueRequest req) {
        requireAdmin();
        return adminConfigService.update(category.toUpperCase(), id, req);
    }

    @DeleteMapping("/{category}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String category, @PathVariable UUID id) {
        requireAdmin();
        adminConfigService.delete(category.toUpperCase(), id);
    }

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }
}
