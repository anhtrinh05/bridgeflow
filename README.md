# BridgeFlow

AI-assisted requirements workspace for Japanese–Vietnamese software teams.

## Current product slice

- Live Project and Requirement REST API backed by PostgreSQL
- Project switcher with create, edit, and archive workflows
- Server-side requirement search, status filters, sorting, pagination, and archive workflow
- OpenAPI contract, Swagger UI, and generated TypeScript API types
- Stateless bearer authentication with project-scoped roles
- Audit events for project and requirement lifecycle changes
- Project-scoped Japanese–Vietnamese glossary with search and role-aware editing
- Project document upload/download with immutable versions and SHA-256 checksums
- Stable requirement identity with append-only bilingual revisions
- BrSE review/confirmation workflow and revision history
- Idempotent AI-generated bilingual clarification questions and acceptance criteria
- Human answers and per-artifact approve/reject audit trail
- Idempotent bilingual test-case drafts generated only from approved acceptance criteria
- Test-case provenance, priority, and human approve/reject audit trail
- Revision-level traceability across source documents, Q&A, criteria, and test cases
- Deterministic change-impact reports with explicit artifact revalidation actions
- Audited cross-requirement relations with incoming/outgoing traceability
- Audited UTF-8 CSV and Markdown project exports
- Correlation-aware structured logs, Prometheus metrics, and liveness/readiness probes
- Synthetic Japanese–Vietnamese demo workspace (no customer data)
- Responsive frontend with loading, error, and empty states

## Repository layout

```text
app/        Next.js frontend
components/ shared frontend components
backend/    Spring Boot REST API
docs/       architecture, security, and data-model decisions
```

## Frontend development

```bash
npm install
npm run dev
```

Open `http://127.0.0.1:5173`.

The frontend calls `http://127.0.0.1:8080/api/v1` by default. Override it with
`NEXT_PUBLIC_API_URL` when the API is hosted elsewhere.

## Backend development

Prerequisites: JDK 21, Maven 3.9+, and PostgreSQL (local or Docker).

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API runs at `http://127.0.0.1:8080`. Check service health at
`/api/v1/health`, liveness at `/actuator/health/liveness`, and database-backed
readiness at `/actuator/health/readiness`.

The `dev` profile creates a synthetic BrSE account for local use:

- Email: `brse@bridgeflow.local`
- Password: `bridgeflow-demo` (override with `BRIDGEFLOW_DEMO_PASSWORD`)

The frontend keeps the opaque access token in `sessionStorage`; only its SHA-256
hash is persisted by the backend, and sessions expire after eight hours.

Open Swagger UI at `http://127.0.0.1:8080/swagger-ui.html` or read the JSON
contract at `http://127.0.0.1:8080/v3/api-docs`. With the backend running,
regenerate frontend API types from the contract with:

```bash
npm run api:generate
```

Run the PostgreSQL integration test with:

```bash
cd backend
mvn test
```

On Windows with PostgreSQL already installed, run this from the repository root
to build and test with a separate temporary database, without Docker:

```powershell
.\scripts\test-backend.cmd
```

The wrapper starts PowerShell with `-ExecutionPolicy Bypass` for that process only,
so no system-wide policy change is needed.

See [backend/README.md](backend/README.md) for details and optional tool paths.

## Production operations

Copy `.env.production.example` to the ignored `.env.production`, replace every
placeholder secret, then start the hardened backend/PostgreSQL stack:

```powershell
docker compose --env-file .env.production -f compose.production.yaml up -d --build
```

The backend binds to `127.0.0.1` by default. Terminate TLS at a trusted reverse
proxy and forward only the API port. PostgreSQL is isolated on an internal
network; database and private documents use named volumes. See
[docs/operations.md](docs/operations.md) for backup, restore, retention, and
release procedures, including the one-shot first-user bootstrap. Production
never enables the development demo account.

## Planned milestones

1. Japanese document ingestion and requirement extraction (implemented)
2. Q&A and acceptance-criteria generation with human review (implemented)
3. Test-case generation from approved criteria (implemented)
4. Requirement traceability and rule-based change-impact analysis (implemented)
5. Cross-requirement relations (implemented)
6. Bilingual requirement exports (implemented)
6A. Production observability foundation (implemented)
7. Production operations:
   - 7A. Deployment packaging and runtime hardening (implemented)
   - 7B. Configurable retention and complete project deletion (implemented)
   - 7C. Verified PostgreSQL/document backup and restore tooling (implemented)
   - 7D. Repeatable release and security verification (implemented)
8. Production readiness and delivery:
   - 8A. Local production-stack smoke verification (verified)
   - 8B. Database/document backup and restore runtime drill (verified)
   - 8C. Secure initial production-user bootstrap (implemented)
   - 8D. GitHub Actions continuous integration (planned)
