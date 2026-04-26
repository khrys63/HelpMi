package com.helpmi.service;

import com.helpmi.domain.Organization;
import com.helpmi.domain.Project;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateOrganizationRequest;
import com.helpmi.dto.request.UpdateOrganizationRequest;
import com.helpmi.dto.response.OrganizationResponse;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listAll() {
        requireAdmin();
        return organizationRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID id) {
        requireAdmin();
        return toResponse(findOrg(id));
    }

    public OrganizationResponse create(CreateOrganizationRequest req) {
        requireAdmin();
        Organization org = Organization.builder().name(req.name().trim()).build();
        return toResponse(organizationRepository.save(org));
    }

    public OrganizationResponse update(UUID id, UpdateOrganizationRequest req) {
        requireAdmin();
        Organization org = findOrg(id);
        if (req.name() != null && !req.name().isBlank()) org.setName(req.name().trim());
        if (req.active() != null) org.setActive(req.active());
        return toResponse(organizationRepository.save(org));
    }

    public void delete(UUID id) {
        requireAdmin();
        Organization org = findOrg(id);
        long userCount = userRepository.countByOrganizationId(id);
        if (userCount > 0) {
            throw new IllegalStateException(
                    "Impossible de supprimer : " + userCount + " utilisateur(s) sont rattachés à cette organisation");
        }
        org.setActive(false);
        organizationRepository.save(org);
    }

    public OrganizationResponse addProject(UUID orgId, UUID projectId) {
        requireAdmin();
        Organization org = findOrgWithProjects(orgId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Projet introuvable"));
        org.getProjects().add(project);
        return toResponse(organizationRepository.save(org));
    }

    public OrganizationResponse removeProject(UUID orgId, UUID projectId) {
        requireAdmin();
        Organization org = findOrgWithProjects(orgId);
        org.getProjects().removeIf(p -> p.getId().equals(projectId));
        return toResponse(organizationRepository.save(org));
    }

    public OrganizationResponse setUserOrganization(UUID orgId, UUID userId) {
        requireAdmin();
        Organization org = findOrgWithProjects(orgId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Les administrateurs n'ont pas d'organisation");
        }
        user.setOrganization(org);
        userRepository.save(user);
        return toResponse(org);
    }

    public OrganizationResponse removeUserFromOrganization(UUID orgId, UUID userId) {
        requireAdmin();
        Organization org = findOrgWithProjects(orgId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (!orgId.equals(user.getOrganization() != null ? user.getOrganization().getId() : null)) {
            throw new IllegalArgumentException("Cet utilisateur n'appartient pas à cette organisation");
        }
        user.setOrganization(null);
        userRepository.save(user);
        return toResponse(org);
    }

    private Organization findOrg(UUID id) {
        return organizationRepository.findById(id)
                .filter(Organization::isActive)
                .orElseThrow(() -> new NotFoundException("Organisation introuvable"));
    }

    private Organization findOrgWithProjects(UUID id) {
        return organizationRepository.findByIdWithProjects(id)
                .filter(Organization::isActive)
                .orElseThrow(() -> new NotFoundException("Organisation introuvable"));
    }

    private OrganizationResponse toResponse(Organization org) {
        List<UserResponse> users = userRepository.findByOrganizationId(org.getId())
                .stream().map(UserResponse::from).toList();
        return OrganizationResponse.from(org, users);
    }

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }
}
