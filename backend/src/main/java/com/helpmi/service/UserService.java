package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.User;
import com.helpmi.domain.UserProject;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.UpdateLocaleRequest;
import com.helpmi.dto.request.UpdateNotificationPrefsRequest;
import com.helpmi.dto.request.UpdateThemeRequest;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    public List<UserResponse> getActiveUsers() {
        User currentUser = currentUserService.getCurrentUser();
        // A3-M3: admins see all active users; others see only users sharing a project
        if (currentUser.getRole() == UserRole.ADMIN) {
            return userRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
                    .stream().map(UserResponse::from).toList();
        }
        return userRepository.findActiveUsersInSameProjects(currentUser.getId())
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
    public UserResponse addOrganization(UUID id, UUID orgId) {
        requireAdmin();
        User user = findUser(id);
        Organization org = organizationRepository.findById(orgId)
                .filter(Organization::isActive)
                .orElseThrow(() -> new NotFoundException("Organisation introuvable"));
        user.getOrganizations().add(org);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse removeOrganization(UUID id, UUID orgId) {
        requireAdmin();
        User user = findUser(id);
        boolean removed = user.getOrganizations().removeIf(o -> o.getId().equals(orgId));
        if (!removed) {
            throw new IllegalArgumentException("Cet utilisateur n'appartient pas à cette organisation");
        }
        Set<UUID> orgProjectIds = new java.util.HashSet<>(projectRepository.findIdsByOrganizationId(orgId));
        user.getUserProjects().removeIf(up -> orgProjectIds.contains(up.getProject().getId()));
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserProjects(UUID id, UpdateUserProjectsRequest req) {
        requireAdmin();
        User user = findUser(id);
        List<UpdateUserProjectsRequest.ProjectRoleEntry> entries =
                req.entries() == null ? List.of() : req.entries();
        if (user.getRole() != UserRole.ADMIN) {
            if (user.getOrganizations().isEmpty()) {
                throw new IllegalArgumentException("L'utilisateur n'a pas d'organisation");
            }
            Set<UUID> allowed = user.getOrganizations().stream()
                    .flatMap(org -> projectRepository.findIdsByOrganizationId(org.getId()).stream())
                    .collect(Collectors.toSet());
            for (var entry : entries) {
                if (!allowed.contains(entry.projectId())) {
                    throw new IllegalArgumentException("Projet non autorisé pour cette organisation");
                }
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
                    .role(entry.role() != null ? entry.role() : "MEMBER")
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

    @Transactional
    public UserResponse updateTheme(UpdateThemeRequest req) {
        User user = currentUserService.getCurrentUser();
        user.setTheme(req.theme());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateLocale(UpdateLocaleRequest req) {
        User user = currentUserService.getCurrentUser();
        user.setLocale(req.locale());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateNotificationPrefs(UpdateNotificationPrefsRequest req) {
        User user = currentUserService.getCurrentUser();
        user.setNotifAssigned(req.notifAssigned());
        user.setNotifComment(req.notifComment());
        user.setNotifStatusChanged(req.notifStatusChanged());
        user.setNotifWatcherAdded(req.notifWatcherAdded());
        user.setNotifTicketCreated(req.notifTicketCreated());
        return UserResponse.from(userRepository.save(user));
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
