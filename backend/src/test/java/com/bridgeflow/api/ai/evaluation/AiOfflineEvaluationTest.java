package com.bridgeflow.api.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bridgeflow.api.ai.application.SensitiveTextRedactor;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.AnalysisRequest;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.ApprovedCriterion;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.ExtractionRequest;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.GlossaryEntry;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.TestCaseRequest;
import com.bridgeflow.api.ai.provider.StubRequirementExtractionProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiOfflineEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StubRequirementExtractionProvider provider;
    private JsonNode corpus;

    @BeforeEach
    void setUp() throws Exception {
        provider = new StubRequirementExtractionProvider();
        var repoRoot = findRepoRoot();
        var corpusFile = repoRoot.resolve("eval/corpus/corpus-v1.json");
        assertThat(Files.exists(corpusFile)).as("Corpus file must exist").isTrue();
        corpus = objectMapper.readTree(corpusFile.toFile());
    }

    private Path findRepoRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("eval/corpus/corpus-v1.json"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root with eval/corpus/corpus-v1.json not found");
    }

    @Test
    @DisplayName("Corpus version and structure conformance")
    void testCorpusStructure() {
        assertThat(corpus.get("version").asText()).isEqualTo("1.0.0");
        assertThat(corpus.get("cases").size()).isGreaterThanOrEqualTo(16);
    }

    @Test
    @DisplayName("Extraction capability and field coverage")
    void testExtractionCapability() {
        var glossary = List.of(new GlossaryEntry("二要素認証", "Xác thực hai yếu tố", "2FA"));
        var docText = "ユーザーはメールアドレスとパスワードでログインできること。\n二要素認証が有効な場合、確認コードの入力を要求すること。";
        var result = provider.extract(new ExtractionRequest(UUID.randomUUID(), docText, glossary, 5));

        assertThat(result.requirements()).hasSize(2);
        for (var req : result.requirements()) {
            assertThat(req.japaneseText()).isNotBlank();
            assertThat(req.vietnameseText()).isNotBlank();
            assertThat(req.sourceAnchor()).startsWith("line:");
        }
    }

    @Test
    @DisplayName("Glossary adherence for Japanese-Vietnamese translation")
    void testGlossaryAdherence() {
        var glossary = List.of(
            new GlossaryEntry("二要素認証", "Xác thực hai yếu tố", "2FA"),
            new GlossaryEntry("監査ログ", "Nhật ký kiểm toán", "Audit")
        );
        var docText = "管理者ログインには二要素認証を必須とし、すべての認証試行を監査ログに記録する。";
        var result = provider.extract(new ExtractionRequest(UUID.randomUUID(), docText, glossary, 5));

        var combinedVi = String.join(" ", result.requirements().stream().map(r -> r.vietnameseText()).toList());
        assertThat(combinedVi).contains("Xác thực hai yếu tố");
        assertThat(combinedVi).contains("Nhật ký kiểm toán");
    }

    @Test
    @DisplayName("Clarification and acceptance criteria synthesis")
    void testAnalysisCapability() {
        var glossary = List.of(new GlossaryEntry("再認証", "Xác thực lại", "Re-auth"));
        var result = provider.analyze(new AnalysisRequest(
            UUID.randomUUID(),
            "セッションの有効期限が切れたら再認証すること。",
            "Khi phiên làm việc hết hạn, yêu cầu xác thực lại.",
            glossary,
            5,
            5
        ));

        assertThat(result.clarificationQuestions()).isNotEmpty();
        for (var q : result.clarificationQuestions()) {
            assertThat(q.japaneseText()).isNotBlank();
            assertThat(q.vietnameseText()).isNotBlank();
            assertThat(q.rationale()).isNotBlank();
        }

        assertThat(result.acceptanceCriteria()).isNotEmpty();
        for (var ac : result.acceptanceCriteria()) {
            assertThat(ac.japaneseText()).isNotBlank();
            assertThat(ac.vietnameseText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Test case generation from approved criteria")
    void testTestCaseGeneration() {
        var criterionId = UUID.randomUUID();
        var approvedCriteria = List.of(new ApprovedCriterion(
            criterionId,
            "前提条件を満たす場合、二要素認証の確認コード画面が表示されること。",
            "Khi đáp ứng điều kiện tiên quyết, màn hình nhập mã xác thực hai yếu tố sẽ hiển thị."
        ));

        var result = provider.generateTestCases(new TestCaseRequest(
            UUID.randomUUID(),
            "二要素認証",
            "Xác thực hai yếu tố",
            approvedCriteria,
            List.of(),
            5
        ));

        assertThat(result.testCases()).isNotEmpty();
        var tc = result.testCases().get(0);
        assertThat(tc.acceptanceCriterionId()).isEqualTo(criterionId);
        assertThat(tc.titleJapanese()).contains("受入条件を検証する");
        assertThat(tc.titleVietnamese()).contains("Xác minh tiêu chí");
        assertThat(tc.stepsJapanese()).isNotBlank();
        assertThat(tc.stepsVietnamese()).isNotBlank();
        assertThat(tc.priority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Sensitive data redaction in prompt preprocessing")
    void testSensitiveDataRedaction() {
        var redactor = new SensitiveTextRedactor("SECRET_TOKEN_INTERNAL_9988");
        var input = "担当者: operator.tokyo@client-corp.jp にお問い合わせください。Token: SECRET_TOKEN_INTERNAL_9988";
        var redacted = redactor.redact(input);

        assertThat(redacted).doesNotContain("operator.tokyo@client-corp.jp");
        assertThat(redacted).doesNotContain("SECRET_TOKEN_INTERNAL_9988");
        assertThat(redacted).contains("[REDACTED_EMAIL]");
        assertThat(redacted).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Provider name and model identification")
    void testProviderIdentity() {
        assertThat(provider.providerName()).isEqualTo("stub");
        assertThat(provider.modelName()).isEqualTo("deterministic-test-provider");
    }
}
