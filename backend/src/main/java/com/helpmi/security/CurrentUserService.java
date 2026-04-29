package com.helpmi.security;

import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    @Transactional
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String keycloakId = jwt.getSubject();
            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseGet(() -> createOrMigrateUserFromJwt(jwt));
            if (!user.isActive()) throw new ForbiddenException("Compte désactivé");
            return user;
        }
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            String email = (String) auth.getPrincipal();
            return userRepository.findByEmail(email)
                    .filter(User::isActive)
                    .orElseThrow(() -> new ForbiddenException("Utilisateur introuvable"));
        }
        throw new ForbiddenException("Authentification requise");
    }

    private User createOrMigrateUserFromJwt(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank())
            throw new IllegalStateException("JWT sans claim 'email' valide : connexion refusée");

        return userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setKeycloakId(jwt.getSubject());
                    existing.setFirstName(nvl(jwt.getClaimAsString("given_name")));
                    existing.setLastName(nvl(jwt.getClaimAsString("family_name")));
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User user = User.builder()
                            .keycloakId(jwt.getSubject())
                            .email(email)
                            .firstName(nvl(jwt.getClaimAsString("given_name")))
                            .lastName(nvl(jwt.getClaimAsString("family_name")))
                            .role(extractRole(jwt))
                            .build();
                    return userRepository.save(user);
                });
    }

    private UserRole extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null && roles.contains("ADMIN")) return UserRole.ADMIN;
        }
        return UserRole.USER;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
