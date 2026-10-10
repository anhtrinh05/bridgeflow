# BridgeFlow Architecture

## Product boundary

BridgeFlow is a requirements workspace for Japanese–Vietnamese delivery teams. The system accepts project documents, extracts structured requirements, supports human review, and maintains traceability from the source document to questions, acceptance criteria, and test cases.

AI output is always a draft. A BrSE or another authorized reviewer must confirm it before it becomes an approved project artifact.

## Target architecture

```text
Browser
  │
  │ HTTPS
  ▼
Caddy gateway
  ├── /          → Vinext frontend bundle
  └── /api/*     → Spring Boot API
                       ├── PostgreSQL
                       ├── Private document volume
                       ├── Background job runner
                       └── AI provider adapter
```

### Frontend

- React App Router-compatible source and TypeScript, built by Vinext/Vite
- Tailwind CSS and shared UI components
- Generated API client from the backend OpenAPI contract
- Production Vinext bundle served by a minimal non-root Node adapter
- Same-origin `/api/v1` calls behind the TLS gateway
- No authoritative project data in browser storage

### Backend

- Java and Spring Boot
- Spring Security with project-level role checks
- Flyway for PostgreSQL schema migrations
- OpenAPI for API documentation and client generation
- One deployable service for the MVP; no microservices

### Persistence

- PostgreSQL stores users, projects, requirements, revisions, jobs, and audit records.
- A private named filesystem volume keeps uploaded source documents; generated
  exports are produced through authenticated project-scoped endpoints.
- Redis and pgvector are deferred until measured product needs justify them.

## Primary data flow

1. An authorized project member uploads a document.
2. The API stores file metadata and the document bytes separately.
3. A persistent job record tracks extraction and AI processing.
4. Extracted requirements are saved as draft revisions with source anchors.
5. A BrSE reviews translations, ambiguities, and proposed questions.
6. Confirmed revisions can be linked to Q&A, acceptance criteria, and test cases.
7. A later document version creates new revisions rather than overwriting history.
8. Traceability views resolve every artifact back to its immutable revision and source.
9. A deterministic impact report identifies prior artifacts that need revalidation after a change.
10. Human-managed relations connect dependencies, replacements, splits, merges, and duplicates across requirements in one project.
11. Authorized exports render the persisted graph as UTF-8 CSV or review-friendly Markdown and record an audit event.

The AI increments execute bounded extraction and per-revision analysis synchronously while persisting
the job lifecycle (`PENDING`, `RUNNING`, `COMPLETED`, or `FAILED`). This keeps the
MVP operationally simple and makes a later move to a background runner possible
without changing the API or losing auditability. Each job targets exactly one
immutable document version or one requirement revision. Revision analysis creates
only draft clarification questions and acceptance criteria; separate human actions
record answers and approve or reject each artifact. A second per-revision job can
generate bilingual test-case drafts only from approved criteria. Each result keeps
its source criterion ID and remains a draft until an authorized human reviews it.

## MVP scope

- Authentication and project membership
- Project and requirement CRUD
- Document upload and version metadata
- Glossary-aware Japanese–Vietnamese analysis
- Human review and confirmation
- Clarification questions and acceptance criteria
- Test-case generation from approved acceptance criteria
- Revision traceability and explainable change-impact reports
- Audited cross-requirement relations
- Audited bilingual CSV and Markdown exports
- Correlation IDs, structured production logs, bounded-cardinality HTTP metrics, and health probes
- Single-origin HTTPS Compose deployment with private frontend/backend/database networks
- Retention deletion receipts and backup/restore operations
- Basic exports and audit history

## Deferred scope

- Multiple AI providers running simultaneously
- Redis-based distributed queues
- pgvector semantic search across large corpora
- Realtime collaborative editing
- Jira, Slack, Teams, and email integrations
- Kubernetes and microservices
