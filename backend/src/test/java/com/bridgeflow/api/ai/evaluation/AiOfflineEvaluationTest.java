package com.bridgeflow.api.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    @Test
    @DisplayName("Versioned corpus executes against production Java components")
    void evaluatesComponentCasesAndWritesMachineEvidence() throws Exception {
        var caseResults = objectMapper.createArrayNode();
        var failures = new ArrayList<String>();
        var glossary = corpusGlossary();
        var componentCases = 0;
        var expectedFields = 0;
        var presentFields = 0;
        var glossaryCases = 0;
        var glossaryCasesPassed = 0;
        var duplicateArtifacts = 0;
        var generatedArtifacts = 0;

        for (var testCase : corpus.withArray("cases")) {
            var capability = testCase.required("capability").asText();
            if (isIntegrationCapability(capability)) continue;
            componentCases++;
            var result = caseResults.addObject();
            result.put("id", testCase.required("id").asText());
            result.put("capability", capability);
            var passed = true;
            var reason = "";
            try {
                switch (capability) {
                    case "extraction" -> {
                        var extraction = provider.extract(new ExtractionRequest(
                            UUID.randomUUID(), testCase.required("documentText").asText(), glossary,
                            testCase.required("maxCandidates").asInt()
                        ));
                        assertThat(extraction.requirements()).hasSizeGreaterThanOrEqualTo(
                            testCase.required("expectedMinCandidates").asInt()
                        );
                        assertThat(extraction.requirements().stream().map(item -> item.sourceAnchor()).toList())
                            .containsExactlyElementsOf(textValues(testCase.withArray("expectedAnchors")));
                        generatedArtifacts += extraction.requirements().size();
                        for (var candidate : extraction.requirements()) {
                            expectedFields += 3;
                            if (candidate.japaneseText() != null && !candidate.japaneseText().isBlank()) presentFields++;
                            if (candidate.vietnameseText() != null && !candidate.vietnameseText().isBlank()) presentFields++;
                            if (candidate.sourceAnchor() != null && !candidate.sourceAnchor().isBlank()) presentFields++;
                        }
                    }
                    case "glossary_adherence" -> {
                        glossaryCases++;
                        assertThat(testCase.required("documentText").asText()).contains(
                            textValues(testCase.withArray("requiredTerms")).toArray(String[]::new)
                        );
                        var extraction = provider.extract(new ExtractionRequest(
                            UUID.randomUUID(), testCase.required("documentText").asText(), glossary, 10
                        ));
                        var combined = String.join(" ", extraction.requirements().stream()
                            .map(item -> item.vietnameseText()).toList());
                        assertThat(combined).contains(
                            textValues(testCase.withArray("expectedVietnameseTerms")).toArray(String[]::new)
                        );
                        glossaryCasesPassed++;
                    }
                    case "clarification_questions" -> {
                        var analysis = provider.analyze(new AnalysisRequest(
                            UUID.randomUUID(), testCase.required("japaneseText").asText(),
                            testCase.required("vietnameseText").asText(), glossary, 8, 12
                        ));
                        assertThat(analysis.clarificationQuestions()).hasSizeGreaterThanOrEqualTo(
                            testCase.required("expectedQuestionsMin").asInt()
                        );
                        if (testCase.path("expectedRationale").asBoolean(false)) {
                            assertThat(analysis.clarificationQuestions())
                                .allSatisfy(question -> assertThat(question.rationale()).isNotBlank());
                        }
                        expectedFields += analysis.clarificationQuestions().size() * 3;
                        for (var question : analysis.clarificationQuestions()) {
                            if (!question.japaneseText().isBlank()) presentFields++;
                            if (!question.vietnameseText().isBlank()) presentFields++;
                            if (!question.rationale().isBlank()) presentFields++;
                        }
                    }
                    case "acceptance_criteria" -> {
                        var analysis = provider.analyze(new AnalysisRequest(
                            UUID.randomUUID(), testCase.required("japaneseText").asText(),
                            testCase.required("vietnameseText").asText(), glossary, 8, 12
                        ));
                        assertThat(analysis.acceptanceCriteria()).hasSizeGreaterThanOrEqualTo(
                            testCase.required("expectedCriteriaMin").asInt()
                        ).allSatisfy(criterion -> {
                            assertThat(criterion.japaneseText()).isNotBlank();
                            assertThat(criterion.vietnameseText()).isNotBlank();
                        });
                        expectedFields += analysis.acceptanceCriteria().size() * 2;
                        presentFields += analysis.acceptanceCriteria().size() * 2;
                    }
                    case "test_cases" -> {
                        var criterion = testCase.required("criterion");
                        var generated = provider.generateTestCases(new TestCaseRequest(
                            UUID.randomUUID(), "評価対象", "Đối tượng đánh giá",
                            List.of(new ApprovedCriterion(
                                UUID.randomUUID(), criterion.required("japaneseText").asText(),
                                criterion.required("vietnameseText").asText()
                            )), glossary, 20
                        ));
                        assertThat(generated.testCases()).hasSize(1);
                        var candidate = generated.testCases().getFirst();
                        for (var field : textValues(testCase.withArray("expectedFields"))) {
                            expectedFields++;
                            var value = switch (field) {
                                case "title" -> candidate.titleJapanese() + candidate.titleVietnamese();
                                case "preconditions" -> candidate.preconditionsJapanese() + candidate.preconditionsVietnamese();
                                case "steps" -> candidate.stepsJapanese() + candidate.stepsVietnamese();
                                case "expectedResult" -> candidate.expectedResultJapanese() + candidate.expectedResultVietnamese();
                                case "priority" -> candidate.priority();
                                default -> throw new AssertionError("Unknown expected test-case field: " + field);
                            };
                            assertThat(value).isNotBlank();
                            presentFields++;
                        }
                    }
                    case "malformed_input" -> {
                        var extraction = provider.extract(new ExtractionRequest(
                            UUID.randomUUID(), testCase.required("documentText").asText(), glossary,
                            testCase.required("maxCandidates").asInt()
                        ));
                        if (testCase.has("expectedCandidates")) {
                            assertThat(extraction.requirements()).hasSize(testCase.required("expectedCandidates").asInt());
                        }
                        if (testCase.has("expectedMaxCandidates")) {
                            assertThat(extraction.requirements().size())
                                .isLessThanOrEqualTo(testCase.required("expectedMaxCandidates").asInt());
                        }
                    }
                    case "sensitive_data_redaction" -> {
                        var terms = testCase.has("redactTerm") ? testCase.required("redactTerm").asText() : "";
                        var redacted = new SensitiveTextRedactor(terms)
                            .redact(testCase.required("documentText").asText());
                        assertThat(redacted)
                            .doesNotContain(testCase.required("forbiddenRaw").asText())
                            .contains(testCase.required("expectedRedacted").asText());
                    }
                    case "duplicate_prevention" -> {
                        assertThat(testCase.required("expectDistinctKeys").asBoolean()).isTrue();
                        var extraction = provider.extract(new ExtractionRequest(
                            UUID.randomUUID(), testCase.required("documentText").asText(), glossary,
                            testCase.required("maxCandidates").asInt()
                        ));
                        var normalized = extraction.requirements().stream()
                            .map(item -> item.japaneseText().strip()).toList();
                        var unique = new HashSet<>(normalized);
                        duplicateArtifacts += normalized.size() - unique.size();
                        generatedArtifacts += normalized.size();
                        assertThat(unique).hasSameSizeAs(normalized);
                        assertThat(normalized).hasSize(testCase.required("expectedUniqueCandidates").asInt());
                    }
                    default -> throw new AssertionError("Unknown component capability: " + capability);
                }
            } catch (AssertionError | RuntimeException exception) {
                passed = false;
                reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                failures.add(testCase.required("id").asText() + ": " + reason);
            }
            result.put("passed", passed);
            result.put("reason", reason);
        }

        var report = objectMapper.createObjectNode();
        report.put("corpusVersion", corpus.required("version").asText());
        report.put("evidenceSource", "production-java-components");
        report.put("providerClass", StubRequirementExtractionProvider.class.getName());
        report.put("redactorClass", SensitiveTextRedactor.class.getName());
        report.put("componentCases", componentCases);
        report.put("passedComponentCases", componentCases - failures.size());
        report.put("expectedFields", expectedFields);
        report.put("presentFields", presentFields);
        report.put("glossaryCases", glossaryCases);
        report.put("glossaryCasesPassed", glossaryCasesPassed);
        report.put("generatedArtifacts", generatedArtifacts);
        report.put("duplicateArtifacts", duplicateArtifacts);
        report.set("caseResults", caseResults);
        var repoRoot = findRepoRoot();
        Files.createDirectories(repoRoot.resolve("target"));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(
            repoRoot.resolve("target/ai-component-evaluation.json").toFile(), report
        );

        assertThat(componentCases).isEqualTo(12);
        assertThat(failures).isEmpty();
    }

    private List<GlossaryEntry> corpusGlossary() {
        var entries = new ArrayList<GlossaryEntry>();
        for (var entry : corpus.withArray("glossary")) {
            entries.add(new GlossaryEntry(
                entry.required("japaneseTerm").asText(),
                entry.required("vietnameseTerm").asText(),
                entry.path("notes").asText("")
            ));
        }
        return entries;
    }

    private List<String> textValues(ArrayNode values) {
        var result = new ArrayList<String>();
        values.forEach(value -> result.add(value.asText()));
        return result;
    }

    private boolean isIntegrationCapability(String capability) {
        return switch (capability) {
            case "timeout_and_failure", "retry_and_idempotency",
                "human_review_enforcement", "traceability_completeness" -> true;
            default -> false;
        };
    }
}
