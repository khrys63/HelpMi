package com.helpmi.controller;

import com.helpmi.dto.request.CreateProjectRequest;
import com.helpmi.dto.request.UpdateProjectRequest;
import com.helpmi.dto.response.ProjectResponse;
import com.helpmi.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> getAll() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ProjectResponse getOne(@PathVariable UUID id) {
        return projectService.getProject(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest req) {
        return projectService.createProject(req);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest req) {
        return projectService.updateProject(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projectService.deleteProject(id);
    }
}
