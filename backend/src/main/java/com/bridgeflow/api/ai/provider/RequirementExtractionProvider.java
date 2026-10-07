package com.bridgeflow.api.ai.provider;

import java.util.List;
import java.util.UUID;

public interface RequirementExtractionProvider {

    ExtractionResult extract(ExtractionRequest request);

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
}
