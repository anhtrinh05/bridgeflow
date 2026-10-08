package com.bridgeflow.api.analysis.api;

import static com.bridgeflow.api.analysis.api.AnalysisModels.AcceptanceCriterionResponse;
import static com.bridgeflow.api.analysis.api.AnalysisModels.AnswerQuestionRequest;
import static com.bridgeflow.api.analysis.api.AnalysisModels.ClarificationQuestionResponse;
import static com.bridgeflow.api.analysis.api.AnalysisModels.RequirementAnalysisResponse;
import static com.bridgeflow.api.analysis.api.AnalysisModels.ReviewArtifactRequest;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.analysis.application.RequirementAnalysisService;
import com.bridgeflow.api.auth.application.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/requirements/{requirementId}/revisions/{revisionId}")
@Tag(name = "Requirement analysis", description = "Human-reviewed AI clarification and acceptance drafts")
@SecurityRequirement(name = "bearerAuth")
public class RequirementAnalysisController {

    private final RequirementAnalysisService service;

    public RequirementAnalysisController(RequirementAnalysisService service) {
        this.service = service;
    }

    @GetMapping("/analysis")
    @Operation(operationId = "getRequirementAnalysis", summary = "Get AI drafts for a requirement revision")
    public RequirementAnalysisResponse get(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID requirementId,
        @PathVariable UUID revisionId
    ) {
        return service.get(user.id(), requirementId, revisionId);
    }

    @PostMapping("/ai-analysis")
    @Operation(operationId = "generateRequirementAnalysis", summary = "Generate AI draft questions and criteria")
    public RequirementAnalysisResponse generate(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID requirementId,
        @PathVariable UUID revisionId
    ) {
        return service.generate(user.id(), requirementId, revisionId);
    }

    @PatchMapping("/questions/{questionId}/answer")
    @Operation(operationId = "answerClarificationQuestion", summary = "Answer a clarification question")
    public ClarificationQuestionResponse answer(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID requirementId,
        @PathVariable UUID revisionId,
        @PathVariable UUID questionId,
        @Valid @RequestBody AnswerQuestionRequest request
    ) {
        return service.answerQuestion(
            user.id(), requirementId, revisionId, questionId, request.japaneseText(), request.vietnameseText()
        );
    }

    @PostMapping("/questions/{questionId}/review")
    @Operation(operationId = "reviewClarificationQuestion", summary = "Approve or reject a clarification question")
    public ClarificationQuestionResponse reviewQuestion(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID requirementId,
        @PathVariable UUID revisionId,
        @PathVariable UUID questionId,
        @Valid @RequestBody ReviewArtifactRequest request
    ) {
        return service.reviewQuestion(user.id(), requirementId, revisionId, questionId, request.decision());
    }

    @PostMapping("/acceptance-criteria/{criterionId}/review")
    @Operation(operationId = "reviewAcceptanceCriterion", summary = "Approve or reject an acceptance criterion")
    public AcceptanceCriterionResponse reviewCriterion(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID requirementId,
        @PathVariable UUID revisionId,
        @PathVariable UUID criterionId,
        @Valid @RequestBody ReviewArtifactRequest request
    ) {
        return service.reviewCriterion(user.id(), requirementId, revisionId, criterionId, request.decision());
    }
}
