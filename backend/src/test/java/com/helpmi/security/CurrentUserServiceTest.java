package com.helpmi.security;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.adminUser;
import static com.helpmi.Fixtures.agentUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks CurrentUserService service;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    // ── JWT path ──────────────────────────────────────────────────────────────

    @Test
    void jwtAuth_existingUser_returnsByKeycloakId() {
        User user = adminUser();
        setJwtAuth(jwtWith("kc-123"));
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        assertThat(service.getCurrentUser()).isSameAs(user);
    }

    @Test
    void jwtAuth_inactiveUser_throwsForbidden() {
        setJwtAuth(jwtWith("kc-123"));
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(inactiveUser()));

        assertThatThrownBy(() -> service.getCurrentUser())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void jwtAuth_knownEmail_migratesToNewKeycloakId() {
        User existing = agentUser();
        Jwt jwt = jwtWithClaims("kc-new", "agent@test.com", "Agent", "User");
        setJwtAuth(jwt);
        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = service.getCurrentUser();

        assertThat(result).isSameAs(existing);
        assertThat(existing.getKeycloakId()).isEqualTo("kc-new");
    }

    @Test
    void jwtAuth_unknownUser_createdFromClaims() {
        Jwt jwt = jwtWithClaims("kc-new", "new@test.com", "New", "User");
        when(jwt.getClaim("realm_access")).thenReturn(null);
        setJwtAuth(jwt);
        User saved = agentUser();
        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(saved);

        assertThat(service.getCurrentUser()).isSameAs(saved);
    }

    @Test
    void jwtAuth_missingEmailClaim_throwsIllegalState() {
        Jwt jwt = jwtWith("kc-123");
        when(jwt.getClaimAsString("email")).thenReturn(null);
        setJwtAuth(jwt);
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email");
    }

    // ── PAT path (fix C1 : était systématiquement 403 avant) ──────────────────

    @Test
    void patAuth_activeUser_returnsUser() {
        User user = agentUser();
        setPatAuth(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThat(service.getCurrentUser()).isSameAs(user);
    }

    @Test
    void patAuth_inactiveUser_throwsForbidden() {
        User user = inactiveUser();
        setPatAuth(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.getCurrentUser())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void patAuth_unknownEmail_throwsForbidden() {
        setPatAuth("nobody@test.com");
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser())
                .isInstanceOf(ForbiddenException.class);
    }

    // ── Pas d'authentification ─────────────────────────────────────────────────

    @Test
    void noAuthentication_throwsForbidden() {
        assertThatThrownBy(() -> service.getCurrentUser())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("requise");
    }

    // ── extractRole — branches ────────────────────────────────────────────────

    @Test
    void jwtAuth_realmAccessWithAdminRole_createsAdminUser() {
        Jwt jwt = jwtWithClaims("kc-admin", "admin@test.com", "Admin", "User");
        Map<String, Object> realmAccess = new java.util.HashMap<>();
        realmAccess.put("roles", List.of("ADMIN", "user"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
        setJwtAuth(jwt);
        when(userRepository.findByKeycloakId("kc-admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.getCurrentUser();

        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void jwtAuth_realmAccessWithoutAdminRole_createsRegularUser() {
        Jwt jwt = jwtWithClaims("kc-user", "user@test.com", "Regular", "User");
        Map<String, Object> realmAccess = new java.util.HashMap<>();
        realmAccess.put("roles", List.of("user", "offline_access"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
        setJwtAuth(jwt);
        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.getCurrentUser();

        assertThat(result.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void jwtAuth_realmAccessRolesNull_createsRegularUser() {
        Jwt jwt = jwtWithClaims("kc-noroles", "noroles@test.com", "No", "Roles");
        Map<String, Object> realmAccess = new java.util.HashMap<>();
        realmAccess.put("roles", null);
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
        setJwtAuth(jwt);
        when(userRepository.findByKeycloakId("kc-noroles")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("noroles@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.getCurrentUser();

        assertThat(result.getRole()).isEqualTo(UserRole.USER);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static Jwt jwtWith(String keycloakId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(keycloakId);
        return jwt;
    }

    private static Jwt jwtWithClaims(String keycloakId, String email, String firstName, String lastName) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(keycloakId);
        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaimAsString("given_name")).thenReturn(firstName);
        when(jwt.getClaimAsString("family_name")).thenReturn(lastName);
        return jwt;
    }

    private static void setJwtAuth(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, Collections.emptyList()));
    }

    private static void setPatAuth(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static User inactiveUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("inactive@test.com")
                .role(UserRole.USER)
                .active(false)
                .build();
    }
}
