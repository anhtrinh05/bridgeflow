package com.bridgeflow.api.ai.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public AnalysisResult analyze(AnalysisRequest request) {
        var japanese = request.japaneseText();
        var vietnamese = applyGlossary(request.japaneseText(), request.vietnameseText(), request.glossary());
        var questions = List.of(
            new ClarificationCandidate(
                "この要件の正常系の完了条件は何ですか？",
                "Điều kiện hoàn tất của luồng thành công cho yêu cầu này là gì?",
                "正常系の判定条件を明確にするため。"
            ),
            new ClarificationCandidate(
                "エラー時の期待動作と表示内容は何ですか？",
                "Khi có lỗi, hành vi và nội dung hiển thị mong đợi là gì?",
                "異常系を実装・検証できる状態にするため。"
            )
        ).stream().limit(request.maxQuestions()).toList();
        var criteria = List.of(
            new AcceptanceCriterionCandidate(
                "前提条件を満たす場合、%s が確認できること。".formatted(japanese),
                "Khi đáp ứng các điều kiện tiên quyết, có thể xác nhận: %s.".formatted(vietnamese)
            ),
            new AcceptanceCriterionCandidate(
                "入力が不正な場合、処理されず理由が表示されること。",
                "Khi dữ liệu nhập không hợp lệ, hệ thống không xử lý và hiển thị lý do."
            )
        ).stream().limit(request.maxCriteria()).toList();
        return new AnalysisResult(questions, criteria);
    }

    @Override
    public String providerName() { return "stub"; }

    @Override
    public String modelName() { return "deterministic-test-provider"; }

    private String applyGlossary(String japanese, String vietnamese, List<GlossaryEntry> glossary) {
        var result = vietnamese;
        for (var term : glossary) {
            if (japanese.contains(term.japaneseTerm()) && !result.contains(term.vietnameseTerm())) {
                result += " (" + term.vietnameseTerm() + ")";
            }
        }
        return result;
    }
}
