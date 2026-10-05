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
mvn spring-boot:run
```

Environment variables can override the development defaults:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/bridgeflow` |
| `DB_USERNAME` | `bridgeflow` |
| `DB_PASSWORD` | `bridgeflow` |
| `SERVER_PORT` | `8080` |

The password above is only for the local Docker database. Use a secret manager in
deployed environments.

## Verify

```bash
mvn test
curl http://127.0.0.1:8080/api/v1/health
curl http://127.0.0.1:8080/actuator/health
```

The integration test starts an isolated PostgreSQL container and verifies both
Flyway migrations and stable requirement/revision persistence.

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
