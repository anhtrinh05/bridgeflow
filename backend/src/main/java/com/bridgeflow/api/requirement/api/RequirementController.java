package com.bridgeflow.api.requirement.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.requirement.api.RequirementModels.ConfirmRevisionRequest;
import com.bridgeflow.api.requirement.api.RequirementModels.CreateRequirementRequest;
import com.bridgeflow.api.requirement.api.RequirementModels.CreateRevisionRequest;
import com.bridgeflow.api.requirement.api.RequirementModels.RequirementResponse;
import com.bridgeflow.api.requirement.api.RequirementModels.RequirementPageResponse;
import com.bridgeflow.api.requirement.application.RequirementService;
import com.bridgeflow.api.requirement.domain.RequirementStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping("/projects/{projectId}/requirements")
    public RequirementPageResponse list(
        @PathVariable UUID projectId,
        @RequestParam(required = false) RequirementStatus status,
        @RequestParam(defaultValue = "false") boolean includeArchived,
        @RequestParam(defaultValue = "") String query,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "displayKey") String sortBy,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page phải >= 0 và size phải từ 1 đến 100.");
        }
        return requirementService.list(
            projectId, status, includeArchived, query, page, size, sortBy, direction
        );
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

    @PostMapping("/requirements/{requirementId}/archive")
    public RequirementResponse archive(@PathVariable UUID requirementId) {
        return requirementService.archive(requirementId);
    }
}
