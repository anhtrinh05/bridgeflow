# Release and Security Checklist

- [ ] `scripts\verify-release.cmd` reports `RELEASE VERIFICATION PASSED`.
- [ ] Flyway reaches the expected version and every integration test passes with
      zero failures, errors, and skipped tests.
- [ ] The production Compose configuration resolves with the deployment env file.
- [ ] `POSTGRES_PASSWORD` and provider keys come from deployment secrets, not Git.
- [ ] The first production user was created through the one-shot bootstrap,
      bootstrap remains disabled during normal runtime, and a second run is rejected.
- [ ] Only synthetic/public fixtures are tracked; no customer document is present.
- [ ] TLS terminates at the approved reverse proxy and the backend remains bound
      to a private or localhost interface.
- [ ] Liveness and database-backed readiness are `UP` after deployment.
- [ ] Metrics require authentication and logs contain no request body or token.
- [ ] A fresh off-host encrypted backup exists.
- [ ] The latest restore drill passed database, document, login, and readiness checks.
- [ ] Retention duration and UTC purge schedule have owner approval.
- [ ] Release commit SHA, operator, date, verification output, and rollback target
      are recorded in the deployment system.
