package com.bridgeflow.api.ai.provider;

import java.util.List;
import java.util.UUID;

public interface RequirementExtractionProvider {

    ExtractionResult extract(ExtractionRequest request);

    AnalysisResult analyze(AnalysisRequest request);

    TestCaseResult generateTestCases(TestCaseRequest request);

    String providerName();

    String modelName();

    record ExtractionRequest(
        UUID correlationId,
        String documentText,
        List<GlossaryEntry> glossary,
        int maxCandidates
    ) {
    }

    record GlossaryEntry(String japaneseTerm, String vietnameseTerm, String notes) {
    }

    record ExtractionResult(List<RequirementCandidate> requirements) {
    }

    record RequirementCandidate(String japaneseText, String vietnameseText, String sourceAnchor) {
    }

    record AnalysisRequest(
        UUID correlationId,
        String japaneseText,
        String vietnameseText,
        List<GlossaryEntry> glossary,
        int maxQuestions,
        int maxCriteria
    ) {
    }

    record AnalysisResult(
        List<ClarificationCandidate> clarificationQuestions,
        List<AcceptanceCriterionCandidate> acceptanceCriteria
    ) {
    }

    record ClarificationCandidate(String japaneseText, String vietnameseText, String rationale) {
    }

    record AcceptanceCriterionCandidate(String japaneseText, String vietnameseText) {
    }

    record TestCaseRequest(
        UUID correlationId,
        String requirementJapanese,
        String requirementVietnamese,
        List<ApprovedCriterion> approvedCriteria,
        List<GlossaryEntry> glossary,
        int maxTestCases
    ) {
    }

    record ApprovedCriterion(UUID id, String japaneseText, String vietnameseText) {
    }

    record TestCaseResult(List<TestCaseCandidate> testCases) {
    }

    record TestCaseCandidate(
        UUID acceptanceCriterionId,
        String titleJapanese,
        String titleVietnamese,
        String preconditionsJapanese,
        String preconditionsVietnamese,
        String stepsJapanese,
        String stepsVietnamese,
        String expectedResultJapanese,
        String expectedResultVietnamese,
        String priority
    ) {
    }
}
