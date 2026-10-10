# Production Operations

## Deployment

1. Copy `.env.production.example` to `.env.production`.
2. Replace `POSTGRES_PASSWORD`; configure AI only when an approved provider and
   key are available. Set `BRIDGEFLOW_SITE_ADDRESS` to the public hostname,
   `BRIDGEFLOW_PUBLIC_ORIGIN` to its exact HTTPS origin, and
   `BRIDGEFLOW_ACME_EMAIL` to the certificate operator. Point public DNS at the
   host and allow inbound TCP 80/443. Never commit `.env.production`.
3. Run:

   ```powershell
   docker compose --env-file .env.production -f compose.production.yaml up -d --build
   docker compose --env-file .env.production -f compose.production.yaml ps
   ```

4. Confirm all four containers are healthy. Open only the configured HTTPS
   origin; the gateway routes `/api/*` to Spring Boot and other requests to the
   frontend Worker. Actuator endpoints intentionally remain private.
5. Confirm login and the core project/requirement workflow through the single
   HTTPS origin. Confirm HTTP redirects to HTTPS and inspect the HSTS,
   `nosniff`, frame-denial, referrer, and permissions-policy headers.

Only Caddy publishes host ports. Frontend, backend, and PostgreSQL have no host
ports; the application and database networks are internal. Frontend and backend
run as non-root users with read-only root filesystems, dropped capabilities, and
bounded temporary filesystems. Named volumes persist PostgreSQL, private
documents, and Caddy certificate state.

### Local TLS-ready drill

Copy `.env.production.local.example` to an ignored `.env.production.local`, set
a disposable PostgreSQL password, then run:

```powershell
$compose = @('compose', '--env-file', '.env.production.local',
  '-f', 'compose.production.yaml', '-f', 'compose.production.local.yaml')
docker @compose up -d --build
docker @compose ps
```

The local override uses Caddy's internal CA and exposes only
`https://localhost:8443` (plus the HTTP redirect port 8081) on loopback. Export
the test root solely for curl/browser verification:

```powershell
docker @compose cp gateway:/data/caddy/pki/authorities/local/root.crt .tooling/caddy-local-root.crt
curl.exe --cacert .tooling/caddy-local-root.crt https://localhost:8443/
curl.exe --cacert .tooling/caddy-local-root.crt https://localhost:8443/api/v1/health
```

Do not install the local CA system-wide. Remove the exported certificate after
the drill. Public production uses `deploy/Caddyfile` and ACME; it must never use
the local override or an internally issued certificate.

For an isolated named drill stack, pass the same Compose project name when
bootstrapping its synthetic operator:

```powershell
.\scripts\bootstrap-production-user.ps1 `
  -ProjectName bridgeflow-m9a `
  -EnvFile .env.production.local `
  -Email operator@bridgeflow.local `
  -DisplayName 'BridgeFlow Local Operator'
```

## Initial production user

The `prod` profile never creates demo credentials. On a fresh database, provision
the first operator through the one-shot backend command:

```powershell
.\scripts\bootstrap-production-user.ps1 `
  -Email operator@example.com `
  -DisplayName "Initial Operator"
```

The script prompts twice for a 16–128 character password, writes it only to a
temporary UTF-8 file, mounts that file read-only into the one-shot container,
and removes it in `finally`. For an approved secret file supplied by an operator
or secret manager, pass `-PasswordFile C:\secure\bridgeflow-bootstrap.txt` and
remove that file after success. The password is never passed on the command line
or written to application logs.

Bootstrap is deliberately refused after any user exists, so it cannot reset or
overwrite credentials. The first user can authenticate through the normal login
endpoint; creating a project makes that user the project's `ADMIN`. Keep normal
backend containers configured with `BRIDGEFLOW_BOOTSTRAP_ENABLED=false` (the
default). Do not add bootstrap values or password files to `.env.production`.

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
