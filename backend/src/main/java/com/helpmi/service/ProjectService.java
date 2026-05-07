package com.helpmi.service;

import com.helpmi.domain.Project;
import com.helpmi.domain.User;
import com.helpmi.domain.UserProject;
import com.helpmi.domain.enums.AuditAction;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateProjectRequest;
import com.helpmi.dto.request.UpdateProjectRequest;
import com.helpmi.dto.response.ProjectResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ProjectRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.repository.UserProjectRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final UserProjectRepository userProjectRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() == UserRole.ADMIN) {
            return projectRepository.findByActiveTrueOrderByCreatedAtDesc()
                    .stream().map(p -> toResponse(p, user)).toList();
        }
        return projectRepository.findActiveByUserId(user.getId())
                .stream().map(p -> toResponse(p, user)).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        Project project = findActive(id);
        requireProjectAccess(project);
        return toResponse(project, currentUserService.getCurrentUser());
    }

    public ProjectResponse createProject(CreateProjectRequest req) {
        requireAdmin();
        User admin = currentUserService.getCurrentUser();
        String key = req.key().toUpperCase();
        if (projectRepository.existsByKey(key)) {
            throw new IllegalArgumentException("La clé de projet existe déjà : " + key);
        }
        Project project = Project.builder()
                .name(req.name())
                .key(key)
                .description(req.description())
                .createdBy(admin)
                .build();
        Project saved = projectRepository.save(project);
        userProjectRepository.save(UserProject.builder()
                .user(admin)
                .project(saved)
                .role("MEMBER")
                .build());
        auditService.log(AuditAction.PROJECT_CREATED, "PROJECT", saved.getKey(), "name=" + saved.getName());
        return toResponse(saved, admin);
    }

    public ProjectResponse updateProject(UUID id, UpdateProjectRequest req) {
        requireAdmin();
        User admin = currentUserService.getCurrentUser();
        Project project = findActive(id);
        if (req.name() != null) project.setName(req.name());
        if (req.description() != null) project.setDescription(req.description());
        return toResponse(projectRepository.save(project), admin);
    }

    public void deleteProject(UUID id) {
        requireAdmin();
        Project project = findActive(id);
        project.setActive(false);
        projectRepository.save(project);
    }

    public ProjectResponse archiveProject(UUID id) {
        requireAdmin();
        Project project = projectRepository.findById(id)
                .filter(Project::isActive)
                .orElseThrow(() -> new NotFoundException("Projet introuvable"));
        project.setArchived(true);
        ProjectResponse resp = toResponse(projectRepository.save(project), currentUserService.getCurrentUser());
        auditService.log(AuditAction.PROJECT_ARCHIVED, "PROJECT", project.getKey(), "name=" + project.getName());
        return resp;
    }

    public ProjectResponse unarchiveProject(UUID id) {
        requireAdmin();
        Project project = projectRepository.findById(id)
                .filter(Project::isActive)
                .orElseThrow(() -> new NotFoundException("Projet introuvable"));
        project.setArchived(false);
        ProjectResponse resp = toResponse(projectRepository.save(project), currentUserService.getCurrentUser());
        auditService.log(AuditAction.PROJECT_UNARCHIVED, "PROJECT", project.getKey(), "name=" + project.getName());
        return resp;
    }

    public String generateTicketReference(UUID projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new NotFoundException("Projet introuvable"));
        project.setTicketSequence(project.getTicketSequence() + 1);
        project = projectRepository.save(project);
        return project.getKey() + "-" + project.getTicketSequence();
    }

    public Project findActive(UUID id) {
        return projectRepository.findById(id)
                .filter(Project::isActive)
                .orElseThrow(() -> new NotFoundException("Projet introuvable : " + id));
    }

    public boolean isGestionnaire(UUID userId, UUID projectId) {
        return userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .map(up -> "MANAGER".equals(up.getRole()))
                .orElse(false);
    }

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }

    public void requireProjectAccess(UUID projectId) {
        requireProjectAccess(findActive(projectId));
    }

    private void requireProjectAccess(Project project) {
        User user = currentUserService.getCurrentUser();
        if (!projectRepository.isProjectAccessibleToUser(project.getId(), user.getId())) {
            throw new ForbiddenException("Accès refusé à ce projet");
        }
    }

    private ProjectResponse toResponse(Project p, User currentUser) {
        long ticketCount = ticketRepository.countByProjectIdAndStatusNotIn(p.getId(), List.of("CLOSED", "CANCELLED", "RESOLVED"));
        boolean canAssign = isGestionnaire(currentUser.getId(), p.getId());
        List<String> orgs = p.getOrganizations().stream()
                .map(o -> o.getName())
                .sorted()
                .toList();
        String userRole = userProjectRepository.findByUserIdAndProjectId(currentUser.getId(), p.getId())
                .map(up -> up.getRole())
                .orElse(null);
        return new ProjectResponse(p.getId(), p.getName(), p.getKey(), p.getDescription(),
                p.getTicketSequence(), ticketCount, p.getCreatedAt(), canAssign, orgs, userRole, p.isArchived());
    }
}
