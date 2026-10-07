package com.bridgeflow.api.ai.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.ai.api.AiModels.AiJobResponse;
import com.bridgeflow.api.ai.application.AiExtractionService;
import com.bridgeflow.api.auth.application.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "AI", description = "Human-reviewed AI extraction jobs")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiExtractionService service;

    public AiController(AiExtractionService service) {
        this.service = service;
    }

    @GetMapping("/projects/{projectId}/ai-jobs")
    @Operation(operationId = "listAiJobs", summary = "List AI jobs for a project")
    public List<AiJobResponse> list(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId
    ) {
        return service.list(user.id(), projectId);
    }

    @PostMapping("/documents/{documentId}/versions/{versionId}/ai-extractions")
    @Operation(operationId = "extractRequirements", summary = "Extract draft requirements from a document version")
    public AiJobResponse extract(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID documentId,
        @PathVariable UUID versionId
    ) {
        return service.extract(user.id(), documentId, versionId);
    }
}
