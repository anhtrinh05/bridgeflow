package com.bridgeflow.api.testcase.api;

import static com.bridgeflow.api.testcase.api.TestCaseModels.ReviewTestCaseRequest;
import static com.bridgeflow.api.testcase.api.TestCaseModels.TestCaseResponse;
import static com.bridgeflow.api.testcase.api.TestCaseModels.TestCaseWorkspaceResponse;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.auth.application.CurrentUser;
import com.bridgeflow.api.testcase.application.TestCaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/requirements/{requirementId}/revisions/{revisionId}/test-cases")
@Tag(name = "Test cases", description = "Human-reviewed bilingual AI test-case drafts")
@SecurityRequirement(name = "bearerAuth")
public class TestCaseController {
    private final TestCaseService service;

    public TestCaseController(TestCaseService service) { this.service = service; }

    @GetMapping
    @Operation(operationId = "getRevisionTestCases", summary = "Get test-case drafts for a requirement revision")
    public TestCaseWorkspaceResponse get(
        @AuthenticationPrincipal CurrentUser user, @PathVariable UUID requirementId, @PathVariable UUID revisionId
    ) { return service.get(user.id(), requirementId, revisionId); }

    @PostMapping("/ai-generation")
    @Operation(operationId = "generateRevisionTestCases", summary = "Generate drafts from approved acceptance criteria")
    public TestCaseWorkspaceResponse generate(
        @AuthenticationPrincipal CurrentUser user, @PathVariable UUID requirementId, @PathVariable UUID revisionId
    ) { return service.generate(user.id(), requirementId, revisionId); }

    @PostMapping("/{testCaseId}/review")
    @Operation(operationId = "reviewTestCase", summary = "Approve or reject a generated test case")
    public TestCaseResponse review(
        @AuthenticationPrincipal CurrentUser user, @PathVariable UUID requirementId, @PathVariable UUID revisionId,
        @PathVariable UUID testCaseId, @Valid @RequestBody ReviewTestCaseRequest request
    ) { return service.review(user.id(), requirementId, revisionId, testCaseId, request.decision()); }
}
