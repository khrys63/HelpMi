package com.helpmi.security;

import com.helpmi.service.PersonalTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.security.disabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class PersonalTokenFilter extends OncePerRequestFilter {

    private final PersonalTokenService personalTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            // JWTs have exactly 2 dots — skip them, let the OAuth2 filter handle them
            if (token.chars().filter(c -> c == '.').count() != 2) {
                personalTokenService.validateToken(token)
                        .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
            }
        }
        chain.doFilter(request, response);
    }
}
