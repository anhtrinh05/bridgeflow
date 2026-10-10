# Release and Security Checklist

- [ ] The GitHub Actions `CI` workflow passed on the exact commit being released.
- [ ] `scripts\verify-release.cmd` reports `RELEASE VERIFICATION PASSED`.
- [ ] Flyway reaches the expected version and every integration test passes with
      zero failures, errors, and skipped tests.
- [ ] The production Compose configuration resolves with the deployment env file.
- [ ] `POSTGRES_PASSWORD` and provider keys come from deployment secrets, not Git.
- [ ] The first production user was created through the one-shot bootstrap,
      bootstrap remains disabled during normal runtime, and a second run is rejected.
- [ ] Only synthetic/public fixtures are tracked; no customer document is present.
- [ ] TLS terminates at the approved reverse proxy and the backend remains bound
      to a private interface; frontend/backend/PostgreSQL publish no host ports.
- [ ] HTTP redirects to the canonical HTTPS origin and gateway security headers
      are present; the local internal-CA override is not deployed publicly.
- [ ] Liveness and database-backed readiness are `UP` after deployment.
- [ ] Metrics require authentication and logs contain no request body or token.
- [ ] A fresh off-host encrypted backup exists.
- [ ] The latest restore drill passed database, document, login, and readiness checks.
- [ ] Retention duration and UTC purge schedule have owner approval.
- [ ] Release commit SHA, operator, date, verification output, and rollback target
      are recorded in the deployment system.

For an on-demand demo release, record the temporary tunnel verification
separately. Do not mark public-production, off-host-backup, DNS, or rollback
items complete merely because a Quick Tunnel demo passed.
