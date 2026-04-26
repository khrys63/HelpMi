package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.AssignOrganizationRequest;
import com.helpmi.dto.request.UpdateUserRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.OrganizationRepository;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;

    public List<UserResponse> getActiveUsers() {
        UserRole role = currentUserService.getCurrentUser().getRole();
        if (role != UserRole.ADMIN && role != UserRole.AGENT) {
            throw new ForbiddenException("Réservé aux administrateurs et agents");
        }
        return userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
                .stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> getAllUsersForAdmin() {
        requireAdmin();
        return userRepository.findAllByOrderByFirstNameAscLastNameAsc()
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        requireAdmin();
        User current = currentUserService.getCurrentUser();
        if (current.getId().equals(id)) {
            throw new ForbiddenException("Vous ne pouvez pas modifier votre propre compte");
        }
        User user = findUser(id);
        if (req.role() != null) user.setRole(req.role());
        if (req.active() != null) user.setActive(req.active());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse assignOrganization(UUID id, AssignOrganizationRequest req) {
        requireAdmin();
        User user = findUser(id);
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Les administrateurs n'ont pas d'organisation");
        }
        if (req.organizationId() == null) {
            user.setOrganization(null);
        } else {
            Organization org = organizationRepository.findById(req.organizationId())
                    .filter(Organization::isActive)
                    .orElseThrow(() -> new NotFoundException("Organisation introuvable"));
            user.setOrganization(org);
        }
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getCurrentUser() {
        return UserResponse.from(currentUserService.getCurrentUser());
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }
}
