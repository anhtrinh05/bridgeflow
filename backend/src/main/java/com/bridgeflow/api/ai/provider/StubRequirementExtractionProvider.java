package com.bridgeflow.api.ai.provider;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bridgeflow.ai.provider", havingValue = "stub")
public class StubRequirementExtractionProvider implements RequirementExtractionProvider {

    @Override
    public ExtractionResult extract(ExtractionRequest request) {
        var candidates = new ArrayList<RequirementCandidate>();
        var lines = Arrays.stream(request.documentText().split("[\\r\\n]+"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .limit(request.maxCandidates())
            .toList();
        for (var index = 0; index < lines.size(); index++) {
            var japanese = lines.get(index);
            var vietnamese = "Bản dịch nháp: " + japanese;
            for (var term : request.glossary()) {
                vietnamese = vietnamese.replace(term.japaneseTerm(), term.vietnameseTerm());
            }
            candidates.add(new RequirementCandidate(japanese, vietnamese, "line:" + (index + 1)));
        }
        return new ExtractionResult(candidates);
    }

    @Override
    public String providerName() { return "stub"; }

    @Override
    public String modelName() { return "deterministic-test-provider"; }
}
