package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.User;
import com.helpmi.domain.UserProject;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.AssignOrganizationRequest;
import com.helpmi.dto.request.UpdateUserProjectsRequest;
import com.helpmi.dto.request.UpdateUserRequest;
import com.helpmi.dto.response.UserResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.OrganizationRepository;
import com.helpmi.repository.ProjectRepository;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    public List<UserResponse> getActiveUsers() {
        currentUserService.getCurrentUser();
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
            user.getUserProjects().clear();
        } else {
            Organization org = organizationRepository.findById(req.organizationId())
                    .filter(Organization::isActive)
                    .orElseThrow(() -> new NotFoundException("Organisation introuvable"));
            UUID currentOrgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
            if (!req.organizationId().equals(currentOrgId)) {
                user.getUserProjects().clear();
            }
            user.setOrganization(org);
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserProjects(UUID id, UpdateUserProjectsRequest req) {
        requireAdmin();
        User user = findUser(id);
        if (user.getOrganization() == null) {
            throw new IllegalArgumentException("L'utilisateur n'a pas d'organisation");
        }
        Set<UUID> allowed = Set.copyOf(
                projectRepository.findIdsByOrganizationId(user.getOrganization().getId()));
        List<UpdateUserProjectsRequest.ProjectRoleEntry> entries =
                req.entries() == null ? List.of() : req.entries();
        for (var entry : entries) {
            if (!allowed.contains(entry.projectId())) {
                throw new IllegalArgumentException("Projet non autorisé pour cette organisation");
            }
        }
        user.getUserProjects().clear();
        userRepository.flush(); // DELETE orphans before INSERT to avoid unique constraint violation
        for (var entry : entries) {
            var project = projectRepository.findById(entry.projectId())
                    .filter(p -> p.isActive())
                    .orElseThrow(() -> new NotFoundException("Projet introuvable"));
            user.getUserProjects().add(UserProject.builder()
                    .user(user)
                    .project(project)
                    .role(entry.role() != null ? entry.role() : "UTILISATEUR")
                    .build());
        }
        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> getAssignableUsers(UUID projectId) {
        return userRepository.findAssignableByProjectId(projectId)
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
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
