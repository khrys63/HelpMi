package com.helpmi.controller;

import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.LabelRequest;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.security.CurrentUserService;
import com.helpmi.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/labels")
@RequiredArgsConstructor
public class AdminLabelController {

    private final LabelService labelService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<LabelResponse> list() {
        return labelService.findAll();
    }

    @GetMapping("/search")
    public List<LabelResponse> search(@RequestParam String q) {
        return labelService.search(q);
    }

    @PostMapping("/find-or-create")
    public LabelResponse findOrCreate(@RequestBody Map<String, String> body) {
        return labelService.findOrCreate(body.getOrDefault("name", ""));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponse create(@Valid @RequestBody LabelRequest req) {
        requireAdmin();
        return labelService.create(req);
    }

    @PutMapping("/{id}")
    public LabelResponse update(@PathVariable UUID id, @Valid @RequestBody LabelRequest req) {
        requireAdmin();
        return labelService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        requireAdmin();
        labelService.delete(id);
    }

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }
}
