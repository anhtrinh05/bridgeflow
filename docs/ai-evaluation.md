# Offline AI quality and safety evaluation

BridgeFlow evaluates its deterministic synthetic provider without calling a
paid model. The gate deliberately separates component evidence from pipeline
evidence so a report cannot pass by recreating the expected result in
JavaScript.

## Evidence model

The versioned corpus is `eval/corpus/corpus-v1.json`. Its 16 synthetic cases
cover 12 capabilities.

- `AiOfflineEvaluationTest` loads the corpus and executes the production Java
  `StubRequirementExtractionProvider` and `SensitiveTextRedactor`. It checks
  exact source anchors, glossary terms, required fields, bounds, redaction, and
  normalized-content duplicate removal. The test writes
  `target/ai-component-evaluation.json`.
- `AiExtractionServiceFailureTest` exercises the real service failure path and
  proves that a provider exception leaves the job failed with no promoted
  requirements or revisions.
- `RequirementPersistenceIntegrationTest` uses PostgreSQL and the HTTP API to
  prove extraction idempotency, draft-only human review, persisted document
  linkage, and revision traceability/change impact.
- `scripts/evaluate-ai-offline.mjs` does not implement an AI provider. It
  requires the Java component report plus named green Surefire test cases,
  joins those artifacts by corpus capability, applies thresholds, and writes
  `target/ai-evaluation-report.json`.

Missing, skipped, failed, stale-version, or renamed evidence makes the
aggregation fail.

## Capabilities and assertions

| Capability | Evidence | Assertion |
| --- | --- | --- |
| Extraction | Production provider | Candidate count, exact anchors, required bilingual fields |
| Glossary adherence | Production provider | Every expected Vietnamese term is present |
| Clarification questions | Production provider | Bilingual question and non-empty rationale |
| Acceptance criteria | Production provider | Bilingual, non-empty criteria; no claim of a formal Gherkin grammar |
| Test cases | Production provider | Titles, preconditions, steps, expected results, priority |
| Malformed input | Production provider | Empty input and maximum-candidate bounds |
| Sensitive-data redaction | Production redactor | Email and configured term are absent after redaction |
| Duplicate prevention | Production provider | Duplicate normalized Japanese lines collapse to one candidate |
| Timeout/provider failure | Service unit test | Failed job, error code, zero promoted drafts |
| Retry/idempotency | PostgreSQL integration test | Same job reused and no duplicate requirements |
| Human review | PostgreSQL integration test | Generated requirement and revision remain `DRAFT` |
| Traceability | PostgreSQL integration test | Persisted revision/artifact links and change impact |

## Thresholds

| Metric | Threshold |
| --- | ---: |
| Safety/redaction cases | 100% |
| Human-review enforcement | 100% |
| Retry/idempotency | 100% |
| Traceability completeness | 100% |
| Explicit glossary cases | 100% |
| Required-field coverage | at least 90% |
| Invalid/duplicate artifacts | at most 5% |

These are deterministic regression metrics for the synthetic stub and the
BridgeFlow pipeline. They are not measurements of a cloud LLM's semantic
quality.

## Running the gate

On Windows, run the self-contained command. It creates a disposable PostgreSQL
cluster, executes the complete backend suite, produces Java/Surefire evidence,
aggregates the report, and removes the cluster after success:

```cmd
scripts\evaluate-ai-offline.cmd
```

or:

```powershell
.\scripts\evaluate-ai-offline.ps1
```

`npm run eval:ai:offline` performs only the final evidence aggregation. Use it
after the backend tests have produced current reports, as the release gate and
CI do.

## Token, cost, and latency boundary

Actual provider spend is exactly `$0` because the evaluated provider is local.
The report includes a deterministic token heuristic and an explicitly labeled
illustrative cost model whose rates are versioned in the corpus. It is neither
a live price quote nor a real-provider token/latency measurement. A genuine
provider evaluation remains blocked until the user separately approves an API
key, model, dataset, and maximum budget.
