package com.helpmi.service;

import com.helpmi.domain.Project;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateProjectRequest;
import com.helpmi.dto.request.UpdateProjectRequest;
import com.helpmi.dto.response.ProjectResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ProjectRepository;
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
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        return toResponse(findActive(id));
    }

    public ProjectResponse createProject(CreateProjectRequest req) {
        requireAdmin();
        String key = req.key().toUpperCase();
        if (projectRepository.existsByKey(key)) {
            throw new IllegalArgumentException("La clé de projet existe déjà : " + key);
        }
        Project project = Project.builder()
                .name(req.name())
                .key(key)
                .description(req.description())
                .createdBy(currentUserService.getCurrentUser())
                .build();
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse updateProject(UUID id, UpdateProjectRequest req) {
        requireAdmin();
        Project project = findActive(id);
        if (req.name() != null) project.setName(req.name());
        if (req.description() != null) project.setDescription(req.description());
        return toResponse(projectRepository.save(project));
    }

    public void deleteProject(UUID id) {
        requireAdmin();
        Project project = findActive(id);
        project.setActive(false);
        projectRepository.save(project);
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

    private void requireAdmin() {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Réservé aux administrateurs");
        }
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getKey(), p.getDescription(), p.getTicketSequence(), p.getCreatedAt());
    }
}
