package com.bridgeflow.api.ai.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bridgeflow.ai.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledRequirementExtractionProvider implements RequirementExtractionProvider {

    @Override
    public ExtractionResult extract(ExtractionRequest request) {
        throw unconfigured();
    }

    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        throw unconfigured();
    }

    @Override
    public TestCaseResult generateTestCases(TestCaseRequest request) {
        throw unconfigured();
    }

    @Override
    public String providerName() { return "disabled"; }

    @Override
    public String modelName() { return "none"; }

    private IllegalStateException unconfigured() {
        return new IllegalStateException(
            "AI provider chưa được cấu hình. Đặt BRIDGEFLOW_AI_PROVIDER=openai và OPENAI_API_KEY trước khi chạy."
        );
    }
}
