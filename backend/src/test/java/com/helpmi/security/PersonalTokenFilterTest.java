package com.helpmi.security;

import com.helpmi.service.PersonalTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalTokenFilterTest {

    @Mock PersonalTokenService personalTokenService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;
    @Mock Authentication authentication;

    @InjectMocks PersonalTokenFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_chainCalledWithoutValidation() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(personalTokenService, never()).validateToken(any());
    }

    @Test
    void nonBearerHeader_chainCalledWithoutValidation() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(personalTokenService, never()).validateToken(any());
    }

    @Test
    void jwtToken_twoDots_skippedByFilter() throws Exception {
        // JWT has exactly 2 dots → filter must not call validateToken
        when(request.getHeader("Authorization")).thenReturn("Bearer header.payload.signature");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(personalTokenService, never()).validateToken(any());
    }

    @Test
    void validPat_setsAuthenticationInContext() throws Exception {
        String pat = "hm_abc123";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + pat);
        when(personalTokenService.validateToken(pat)).thenReturn(Optional.of(authentication));

        filter.doFilterInternal(request, response, chain);

        assertAuthenticationSet(authentication);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidPat_unknownToken_doesNotSetAuthentication() throws Exception {
        String pat = "hm_unknown";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + pat);
        when(personalTokenService.validateToken(pat)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        assertNoAuthentication();
        verify(chain).doFilter(request, response);
    }

    @Test
    void alreadyAuthenticated_patValidationSkipped() throws Exception {
        SecurityContext ctx = SecurityContextHolder.getContext();
        ctx.setAuthentication(authentication);

        when(request.getHeader("Authorization")).thenReturn("Bearer hm_abc");

        filter.doFilterInternal(request, response, chain);

        verify(personalTokenService, never()).validateToken(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void chainAlwaysCalled_evenWhenValidationFails() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer hm_bad");
        when(personalTokenService.validateToken(any())).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private void assertAuthenticationSet(Authentication expected) {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(expected);
    }

    private void assertNoAuthentication() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
