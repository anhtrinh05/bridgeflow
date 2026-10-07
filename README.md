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

The API runs at `http://127.0.0.1:8080`. Check liveness at
`/api/v1/health` and database readiness at `/actuator/health`.

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

## Planned milestones

1. Japanese document ingestion and requirement extraction
2. Q&A, acceptance criteria, and test-case generation
3. Requirement traceability, change-impact analysis, and production observability
