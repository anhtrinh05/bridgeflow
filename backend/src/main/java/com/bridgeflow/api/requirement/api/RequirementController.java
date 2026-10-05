package com.bridgeflow.api.requirement.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.requirement.api.RequirementModels.ConfirmRevisionRequest;
import com.bridgeflow.api.requirement.api.RequirementModels.CreateRequirementRequest;
import com.bridgeflow.api.requirement.api.RequirementModels.CreateRevisionRequest;
import com.bridgeflow.api.requirement.api.RequirementModels.RequirementResponse;
import com.bridgeflow.api.requirement.application.RequirementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping("/projects/{projectId}/requirements")
    public List<RequirementResponse> list(@PathVariable UUID projectId) {
        return requirementService.list(projectId);
    }

    @PostMapping("/projects/{projectId}/requirements")
    public ResponseEntity<RequirementResponse> create(
        @PathVariable UUID projectId, @Valid @RequestBody CreateRequirementRequest request
    ) {
        var requirement = requirementService.create(projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/requirements/" + requirement.id())).body(requirement);
    }

    @GetMapping("/requirements/{requirementId}")
    public RequirementResponse get(@PathVariable UUID requirementId) {
        return requirementService.get(requirementId);
    }

    @PostMapping("/requirements/{requirementId}/revisions")
    public ResponseEntity<RequirementResponse> addRevision(
        @PathVariable UUID requirementId, @Valid @RequestBody CreateRevisionRequest request
    ) {
        return ResponseEntity.ok(requirementService.addRevision(requirementId, request));
    }

    @PostMapping("/requirements/{requirementId}/revisions/{revisionId}/confirm")
    public RequirementResponse confirm(
        @PathVariable UUID requirementId,
        @PathVariable UUID revisionId,
        @Valid @RequestBody ConfirmRevisionRequest request
    ) {
        return requirementService.confirm(requirementId, revisionId, request);
    }
}
