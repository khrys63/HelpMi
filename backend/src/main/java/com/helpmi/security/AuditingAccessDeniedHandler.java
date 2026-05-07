package com.helpmi.security;

import com.helpmi.domain.enums.AuditAction;
import com.helpmi.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditingAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditService auditService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        String path = request.getRequestURI();
        auditService.log(AuditAction.ACCESS_DENIED, null, email, "ENDPOINT", path,
                "method=" + request.getMethod());
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
