package com.helpmi.controller;

import com.helpmi.domain.enums.AuditAction;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.response.AuditLogResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.repository.AuditLogRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<AuditLogResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) AuditAction action
    ) {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
        size = Math.min(size, 200);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (action != null) {
            return auditLogRepository.findByAction(action, pageable).map(AuditLogResponse::from);
        }
        return auditLogRepository.findAll(pageable).map(AuditLogResponse::from);
    }
}
