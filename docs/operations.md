# Production Operations

## Deployment

1. Copy `.env.production.example` to `.env.production`.
2. Replace `POSTGRES_PASSWORD`; configure AI only when an approved provider and
   key are available. Set `BRIDGEFLOW_CORS_ALLOWED_ORIGINS` to the exact HTTPS
   frontend origin. Never commit `.env.production`.
3. Run:

   ```powershell
   docker compose --env-file .env.production -f compose.production.yaml up -d --build
   docker compose --env-file .env.production -f compose.production.yaml ps
   ```

4. Put a trusted TLS reverse proxy in front of the default localhost binding.
5. Confirm both `/actuator/health/liveness` and
   `/actuator/health/readiness` return `UP` through the deployed route.

PostgreSQL has no published port. The backend runs as a non-root user with a
read-only root filesystem, dropped capabilities, a bounded temporary filesystem,
and persistent named volumes only for PostgreSQL and private documents.

## Retention and deletion

Manual deletion requires an authenticated project `ADMIN`, an archived project,
and exact code confirmation:

```http
DELETE /api/v1/projects/{projectId}
Content-Type: application/json
Authorization: Bearer <token>

{"confirmationCode":"PROJECT-CODE"}
```

Set `BRIDGEFLOW_RETENTION_ENABLED=true` to purge archived projects after
`BRIDGEFLOW_RETENTION_ARCHIVED_PROJECT_DAYS`. The cron expression runs in UTC.
Each manual or scheduled deletion leaves a content-free deletion receipt.

## Backup

The backup script briefly stops only the backend so PostgreSQL and document
storage represent the same application checkpoint. PostgreSQL remains running.

```powershell
.\scripts\backup-production.ps1
```

Each timestamped backup contains `database.sql`, `documents/`, and
`manifest.json`. Store the directory on encrypted off-host storage with access
controls and a retention policy. A backup left only beside the production host
does not protect against host loss.

## Restore drill

Restoring replaces the production database and document volume. Take a fresh
backup first, select the exact backup directory, and provide the required literal
confirmation:

```powershell
.\scripts\restore-production.ps1 `
  -BackupDirectory .\backups\20261009-230000 `
  -Confirmation RESTORE
```

The script verifies the database SHA-256 from the manifest, stops the backend,
restores PostgreSQL and documents, starts the backend, and requires readiness to
pass. Run a restore drill after deployment changes and at least quarterly. Keep
the drill record, recovery time, backup timestamp, and readiness result.

## Release gate

Run from a clean checkout:

```cmd
scripts\verify-release.cmd
```

Release only when backend integration tests, frontend lint/type-check/build,
and production Compose validation all pass. Complete the checklist in
`docs/release-checklist.md` and verify no real customer files, credentials, or
local `.env.production` file are tracked.
