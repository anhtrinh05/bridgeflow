# BridgeFlow v1.0.1 corrective release notes

Status: release candidate pending authoritative Windows/Docker/fresh-clone
verification. Do not create or move a tag until every gate passes.

## Why this correction exists

An independent post-release audit found that v1.0.0's JavaScript AI evaluator
reimplemented the deterministic provider and represented four pipeline
invariants with constant objects. The application runtime was not broken, but
the published 100% evidence claims were stronger than the evaluator justified.
The audit also found stale handoff state, several unsupported portfolio claims,
and Markdown trailing whitespace.

## Corrections

- Execute corpus component cases against the production Java provider and
  redactor rather than a JavaScript copy.
- Require named passing Surefire evidence for provider failure isolation,
  extraction idempotency, draft-only human review, and persisted traceability.
- Add a real service failure test proving zero promoted requirements/revisions.
- Remove duplicate normalized source lines in the deterministic stub provider
  and assert the corpus's expected unique candidate count.
- Add a tracked secret-pattern gate to local release verification and CI.
- Label token/cost output as an illustrative versioned model, not a live quote
  or real-provider measurement.
- Correct Caddy user, bearer-token, frontend, encryption, provider, and
  evaluation claims in the portfolio documentation.
- Repair stale handoff state and require the previously omitted full fresh-clone
  runtime audit before publication.

## Required publication gate

The verifier must run the complete release gate with all tests executed and no
failures, errors, or skips; run the evidence aggregation twice with equivalent
case/metric output; perform production image, local HTTPS, bootstrap refusal,
backup/restore, and Quick Tunnel browser drills from a disposable fresh clone;
verify GitHub Actions on the exact candidate SHA; and confirm that `v1.0.0`
remains unchanged. Only then may a new annotated `v1.0.1` tag be created.
