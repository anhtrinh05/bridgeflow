package com.bridgeflow.api.requirement.api;

import static com.bridgeflow.api.requirement.api.RequirementRelationModels.CreateRequirementRelationRequest;
import static com.bridgeflow.api.requirement.api.RequirementRelationModels.RequirementRelationResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.auth.application.CurrentUser;
import com.bridgeflow.api.requirement.application.RequirementRelationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requirements/{requirementId}/relations")
@Tag(name = "Requirement relations", description = "Human-managed cross-requirement traceability")
@SecurityRequirement(name = "bearerAuth")
public class RequirementRelationController {
    private final RequirementRelationService service;

    public RequirementRelationController(RequirementRelationService service) { this.service = service; }

    @GetMapping
    @Operation(operationId = "listRequirementRelations", summary = "List incoming and outgoing requirement relations")
    public List<RequirementRelationResponse> list(
        @AuthenticationPrincipal CurrentUser user, @PathVariable UUID requirementId
    ) { return service.list(user.id(), requirementId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createRequirementRelation", summary = "Create a project-scoped requirement relation")
    public RequirementRelationResponse create(
        @AuthenticationPrincipal CurrentUser user, @PathVariable UUID requirementId,
        @Valid @RequestBody CreateRequirementRelationRequest request
    ) { return service.create(user.id(), requirementId, request); }

    @DeleteMapping("/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteRequirementRelation", summary = "Delete a requirement relation")
    public void delete(
        @AuthenticationPrincipal CurrentUser user, @PathVariable UUID requirementId, @PathVariable UUID relationId
    ) { service.delete(user.id(), requirementId, relationId); }
}
