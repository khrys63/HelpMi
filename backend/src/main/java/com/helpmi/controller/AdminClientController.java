package com.helpmi.controller;

import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.ClientRequest;
import com.helpmi.dto.response.ClientResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.security.CurrentUserService;
import com.helpmi.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientController {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<ClientResponse> list() {
        return clientService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest req) {
        requireAdmin();
        return clientService.create(req);
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest req) {
        requireAdmin();
        return clientService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        requireAdmin();
        clientService.delete(id);
    }

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }
}
