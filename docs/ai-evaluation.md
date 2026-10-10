# Offline AI Quality & Safety Evaluation (Milestone 10A)

This document specifies the methodology, synthetic corpus, quality gates, safety invariants, and offline evaluation harness for BridgeFlow's bilingual Japanese–Vietnamese AI requirements workspace.

## 1. Overview & Evaluation Principles

1. **Zero Recurring Cost & Zero API Spend**:
   All automated evaluation is run offline and deterministically using synthetic test specifications and the deterministic stub provider (`StubRequirementExtractionProvider`). No real LLM API keys, payment cards, or external network requests are needed.
2. **Deterministic Reproducibility**:
   Given identical corpus inputs, the evaluation suite produces bit-for-bit identical capability results, metrics, and token calculations across multiple runs.
3. **Safety & Human-in-the-Loop Enforced**:
   AI extraction outputs are strictly treated as **untrusted drafts** (`DRAFT` status and `DRAFT` review status). The system strictly prohibits auto-confirmation of AI candidates.
4. **Data Privacy & Redaction**:
   Sensitive information (such as operator emails and proprietary internal tokens) is redacted before prompt dispatch. Raw sensitive data never appears in prompts or logs.
5. **Traceability by Design**:
   Every synthesized candidate links to a source document version and a source anchor (`line:N`).

## 2. Evaluation Corpus (`eval/corpus/corpus-v1.json`)

The evaluation dataset is versioned (`version: 1.0.0`) and contains synthetic requirement specifications for Japanese–Vietnamese software development scenarios (such as authentication, session management, RBAC, and audit logs).

### Supported Capabilities Tested

| Capability | Description | Target Assertions |
| :--- | :--- | :--- |
| `extraction` | Document text -> bilingual requirement candidates | Complete candidates, source anchors, field presence |
| `glossary_adherence` | Strict translation of domain terms (2FA, audit log, RBAC, PII) | 100% presence of expected Vietnamese glossary terms |
| `clarification_questions` | Ambiguity detection and question generation | Meaningful questions with rationales |
| `acceptance_criteria` | Generation of verifiable preconditions and criteria | Given/When/Then criteria structure |
| `test_cases` | Generation of test cases from approved criteria | Steps, preconditions, expected results, priority |
| `malformed_input` | Handling empty, whitespace, and oversized inputs | Graceful degradation, clamping to bounds |
| `sensitive_data_redaction` | Email and token pattern scrubbing | 100% redacted to `[REDACTED_EMAIL]` / `[REDACTED]` |
| `timeout_and_failure` | Provider exceptions or timeouts | Job transitions to `FAILED`; zero drafts promoted |
| `retry_and_idempotency` | Re-running extraction on existing version | Reuses existing job; zero duplicate requirements |
| `duplicate_prevention` | Deduplication of repeated inputs | Bounded by max candidates, distinct keys |
| `human_review_enforcement` | Mandatory draft state on all generated items | Initial status is `DRAFT`; 0 auto-confirmed items |
| `traceability_completeness` | Source anchor and version linkage | 100% candidates linked to source line & version |

## 3. Thresholds & Target Metrics

| Metric | Target Threshold | Description |
| :--- | :---: | :--- |
| **Safety & Redaction Pass Rate** | **100.0%** | Zero sensitive data leaks into prompts or outputs |
| **Human Review Enforcement Rate** | **100.0%** | 100% of generated requirements require human confirmation |
| **Retry & Idempotency Rate** | **100.0%** | Re-running does not produce duplicate database entities |
| **Traceability Completeness** | **100.0%** | All generated items maintain document version & source anchor |
| **Glossary Adherence Rate** | **100.0%** | Explicit glossary terms are accurately translated |
| **Expected Field Coverage** | **>= 90.0%** | All required bilingual fields are populated |
| **Invalid / Duplicate Rate** | **<= 5.0%** | Generated candidate keys are distinct and valid |

## 4. Deterministic Cost & Latency Modeling

Although the offline evaluation executes against the deterministic local stub ($0.00 actual cost), the harness models token consumption and hypothetical cost against modern cloud LLMs (e.g., OpenAI `gpt-4o-mini` at $0.15/1M prompt tokens and $0.60/1M completion tokens):

- **Prompt Tokens**: Deterministically estimated using standard CJK/Latin token ratios (~1 token / 1.5 CJK characters, ~1 token / 4 Latin characters).
- **Completion Tokens**: Estimated based on structured output schema size.
- **Hypothetical Cost**: ~$0.0007 per complete 16-case test suite run.
- **Execution Latency**: Typically < 50ms for the entire offline suite.

## 5. Running the Evaluation

### Via NPM
```bash
npm run eval:ai:offline
```

### Via PowerShell / Windows CLI
```powershell
.\scripts\evaluate-ai-offline.ps1
```

```cmd
scripts\evaluate-ai-offline.cmd
```

### Via Backend Maven Test Suite
```bash
mvn test -Dtest=AiOfflineEvaluationTest
```

The harness writes a machine-readable JSON report to `target/ai-evaluation-report.json`.
