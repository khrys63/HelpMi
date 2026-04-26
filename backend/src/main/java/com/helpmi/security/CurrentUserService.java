package com.helpmi.security;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String keycloakId = jwt.getSubject();
            return userRepository.findByKeycloakId(keycloakId)
                    .orElseGet(() -> createUserFromJwt(jwt));
        }

        // dev mode: principal is the email string set by DevAuthFilter
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Dev user not found: " + email));
    }

    private User createUserFromJwt(Jwt jwt) {
        User user = User.builder()
                .keycloakId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .firstName(nvl(jwt.getClaimAsString("given_name")))
                .lastName(nvl(jwt.getClaimAsString("family_name")))
                .role(extractRole(jwt))
                .build();
        return userRepository.save(user);
    }

    private UserRole extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                if (roles.contains("ADMIN")) return UserRole.ADMIN;
                if (roles.contains("AGENT")) return UserRole.AGENT;
            }
        }
        return UserRole.CLIENT;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
