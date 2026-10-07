# BridgeFlow API

Spring Boot API and PostgreSQL persistence layer for BridgeFlow.

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- Docker Desktop for container-based tests, or a local PostgreSQL installation

## Run locally

From the repository root:

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Environment variables can override the development defaults:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/bridgeflow` |
| `DB_USERNAME` | `bridgeflow` |
| `DB_PASSWORD` | `bridgeflow` |
| `SERVER_PORT` | `8080` |
| `BRIDGEFLOW_DEMO_PASSWORD` | `bridgeflow-demo` |
| `BRIDGEFLOW_STORAGE_ROOT` | OS temp directory under `bridgeflow-uploads` |
| `BRIDGEFLOW_STORAGE_MAX_BYTES` | `10485760` (10 MiB) |

The password above is only for the local Docker database. Use a secret manager in
deployed environments.

## REST API

Except for login, health checks, and API documentation, endpoints require an
`Authorization: Bearer <token>` header. The `dev` profile creates
`brse@bridgeflow.local` with the password configured by
`BRIDGEFLOW_DEMO_PASSWORD` and grants it the `BRSE` role on the synthetic project.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Exchange email/password for an eight-hour opaque access token |
| `GET` | `/api/v1/auth/me` | Read the authenticated user |
| `POST` | `/api/v1/auth/logout` | Revoke the current access token |

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` / `POST` | `/api/v1/projects` | List or create projects |
| `GET` | `/api/v1/projects/{projectId}` | Read a project |
| `PATCH` | `/api/v1/projects/{projectId}` | Update project details |
| `POST` | `/api/v1/projects/{projectId}/archive` | Archive a project |
| `GET` / `POST` | `/api/v1/projects/{projectId}/glossary` | Search or create glossary terms |
| `PATCH` / `DELETE` | `/api/v1/projects/{projectId}/glossary/{termId}` | Update or delete a glossary term |
| `GET` / `POST` | `/api/v1/projects/{projectId}/documents` | List or upload project documents |
| `POST` | `/api/v1/documents/{documentId}/versions` | Upload an immutable new version |
| `GET` | `/api/v1/documents/{documentId}/versions/{versionId}/content` | Download an authorized version |
| `POST` | `/api/v1/documents/{documentId}/archive` | Archive a document |
| `GET` / `POST` | `/api/v1/projects/{projectId}/requirements` | Search/page or create requirements |
| `GET` | `/api/v1/requirements/{requirementId}` | Read a requirement and its revision history |
| `POST` | `/api/v1/requirements/{requirementId}/revisions` | Add a bilingual revision |
| `POST` | `/api/v1/requirements/{requirementId}/revisions/{revisionId}/confirm` | Confirm the latest revision |
| `POST` | `/api/v1/requirements/{requirementId}/archive` | Archive a requirement |

Requirement listing supports `status`, `query`, `page`, `size`, `sortBy`,
`direction`, and `includeArchived` query parameters. Archived requirements are
excluded by default and retain their stable ID and complete revision history.

Project roles are enforced by the API: `ADMIN` manages the project,
`ADMIN`/`BRSE` confirm and archive requirements,
`ADMIN`/`BRSE`/`DEVELOPER` create requirements and revisions, and `VIEWER` has
read-only access. `ADMIN` and `BRSE` manage glossary terms; every project,
requirement, and glossary mutation creates an audit event tied to the
authenticated user.

Document bytes are stored outside PostgreSQL beneath `BRIDGEFLOW_STORAGE_ROOT`;
the database stores version metadata, an opaque storage key, and SHA-256. Uploads
are limited to 10 MiB and PDF, DOCX, TXT, or Markdown. `ADMIN`, `BRSE`, and
`DEVELOPER` can upload versions; only `ADMIN` and `BRSE` can archive documents.
Production deployments should point the storage adapter at a durable private
volume and add malware scanning before ingestion.

Interactive documentation is available at `/swagger-ui.html`, and the machine-readable
OpenAPI contract used by the frontend generator is available at `/v3/api-docs`.

The default profile only loads production-safe schema migrations from
`db/migration`. The `dev` profile additionally loads synthetic
Japanese–Vietnamese portfolio data from `db/devdata`; it contains no customer
documents or NDA-protected data.

## Verify

```bash
mvn test
curl http://127.0.0.1:8080/api/v1/health
curl http://127.0.0.1:8080/actuator/health
```

The integration test starts an isolated PostgreSQL container and verifies the
production-safe Flyway migrations and stable requirement/revision persistence.

### Windows without Docker

From the repository root, run:

```powershell
.\scripts\test-backend.cmd
```

The `.cmd` wrapper runs `scripts\test-backend.ps1` through
`powershell -NoProfile -ExecutionPolicy Bypass -File`, so it works even when the
execution policy blocks `.ps1` files, without changing that policy.

The script uses the installed PostgreSQL binaries to create a new cluster on a
free localhost port, creates `bridgeflow_test`, runs `mvn clean verify`, and stops
its server in a `finally` block. Existing servers and databases are not used.
After a successful run the temporary cluster is deleted; after a failure it is
kept under the ignored `.tooling` directory together with `postgres.log`. Java and
database environment variables are restored after the run.

Optional parameters: `-JavaHome`, `-MavenCommand`, `-PostgresBin`, and
`-KeepCluster` (keep the cluster directory even on success).

For other environments, the tests accept `BRIDGEFLOW_TEST_DB_URL`,
`BRIDGEFLOW_TEST_DB_USERNAME`, and `BRIDGEFLOW_TEST_DB_PASSWORD`. Always use a
disposable database because Flyway applies migrations when the tests start.
When those variables are absent, Docker is required; tests fail rather than
silently skipping database verification.
