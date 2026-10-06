package com.bridgeflow.api.project.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.project.api.ProjectModels.CreateProjectRequest;
import com.bridgeflow.api.project.api.ProjectModels.ProjectResponse;
import com.bridgeflow.api.project.api.ProjectModels.UpdateProjectRequest;
import com.bridgeflow.api.project.application.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> list(
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return projectService.list(includeArchived);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return projectService.get(projectId);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        var project = projectService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + project.id())).body(project);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse update(
        @PathVariable UUID projectId,
        @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.update(projectId, request);
    }

    @PostMapping("/{projectId}/archive")
    public ProjectResponse archive(@PathVariable UUID projectId) {
        return projectService.archive(projectId);
    }
}
