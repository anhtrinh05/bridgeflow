# Security and Data Handling

## Security goals

BridgeFlow may process confidential specifications. The MVP must minimize what is sent to external AI providers, restrict access by project, and leave an auditable trail for sensitive operations.

Demo and evaluation environments must use synthetic documents only. Real customer documents must never be committed to Git or included in public test fixtures.

## Data classification

| Class | Examples | Handling |
| --- | --- | --- |
| Public | Product documentation and synthetic samples | May be stored in the public repository |
| Internal | Project configuration and non-sensitive test output | Authenticated access only |
| Confidential | Customer specifications, Q&A, exports | Project-scoped authorization and audit logging |
| Restricted | Credentials, tokens, personal data | Never logged or committed; redact where possible |

## Required controls

- TLS for every external connection.
- Server-side authorization on every project resource.
- Object keys that cannot be guessed from filenames.
- Short-lived download links for private documents.
- Encryption at rest provided by the selected storage services.
- Audit events for uploads, downloads, AI processing, approvals, and deletion.
- File type, size, and malware validation before processing.
- Secrets supplied through runtime configuration, never source control.
- Configurable retention and complete project deletion.

Permanent deletion is restricted to project `ADMIN`, requires the project to
be archived, and requires its exact code as explicit confirmation. Project data
is deleted transactionally through database cascades; private document storage
is cleaned only after commit. A minimal deletion receipt deliberately survives
without customer content so operators retain evidence of manual and retention
deletions. Automated retention is disabled unless production configuration
explicitly enables it.

## Implemented authentication boundary

- Login returns a cryptographically random opaque bearer token with an eight-hour expiry.
- PostgreSQL stores only the token's SHA-256 hash; raw tokens and passwords are never logged.
- Passwords use Spring Security's delegating password encoder with bcrypt as the default.
- The browser keeps the token in `sessionStorage`, so closing the tab ends the local session.
- API authorization resolves project membership on every project or requirement request.
- Roles are `ADMIN`, `BRSE`, `DEVELOPER`, and read-only `VIEWER`.
- Project and requirement create, update, confirm, archive, and revision actions write an
  immutable audit event with the authenticated actor ID.
- Document storage keys are generated UUID paths and never derived from user filenames.
- Uploads enforce a 10 MiB limit, an allowlist of PDF/DOCX/text formats, basic content
  signature checks, SHA-256 hashing, project authorization, and audit events.
- Original filenames are used only for download presentation; normalized paths remain
  confined beneath the configured private storage root.

## AI provider boundary

Before sending content to an AI provider, the backend must:

1. Confirm that AI processing is enabled for the project.
2. Verify the requesting user's project role.
3. Remove unrelated document sections and redact configured sensitive fields.
4. Record the provider, model, purpose, immutable target version/revision, and requesting user.
5. Send only the minimum text required for the current operation.

The selected provider must have documented retention and training controls suitable for the deployment. Provider responses remain untrusted drafts until reviewed by a human.

The zero-cost portfolio demo uses an anonymous, temporary Cloudflare Quick
Tunnel and synthetic data only. Public TLS terminates at Cloudflare; the tunnel
connects to a loopback-bound Caddy demo origin, while frontend, backend,
PostgreSQL, and document volumes remain unpublished. The random URL is not an
access-control mechanism: normal login and project authorization remain
mandatory. Caddy marks both upstream hops with the original public HTTPS scheme
so Spring's forwarded-header and same-origin checks remain correct. Stop the
tunnel after the demo and never upload customer documents.

The OpenAI adapter uses the Responses API with Structured Outputs, disables provider
storage (`store: false`), sends a per-job correlation ID, and reads the API key only
from the process environment. Project AI processing is disabled by default. The
backend strips email addresses and configured literal sensitive values before the
request, applies a character budget, and never stores or logs raw prompts.
Requirement-analysis prompts contain only the selected bilingual revision and the
project glossary, not unrelated documents. Structured output is size-bounded and
validated before persistence. AI output remains `DRAFT` until an `ADMIN` or `BRSE`
records an explicit review decision; provider output cannot confirm a requirement.

## Logging rules

- Log identifiers and operation results, not full document text.
- Never log access tokens, passwords, signed URLs, or raw AI prompts containing customer data.
- Give every request and AI job a correlation ID.
- Keep security and audit logs separate from user-visible activity history.

The request observability filter validates `X-Correlation-ID`, generates a UUID
when needed, returns it on every response, and clears MDC after the request.
Access logs deliberately exclude query strings, headers, bodies, tokens, and
document text. HTTP metrics use Spring route templates (or UUID-redacted fallback
paths) so customer/resource identifiers never become metric labels. Prometheus
and detailed metric endpoints require authentication; only health probes are public.

The production Compose stack keeps PostgreSQL on an internal-only network,
runs the backend as a non-root user with a read-only root filesystem and dropped
Linux capabilities, persists only database/document volumes, and binds the API
to localhost by default. TLS remains the responsibility of the external reverse
proxy. Production disables Swagger/OpenAPI endpoints and enables graceful shutdown.

The production gateway is the sole published network entry point. It terminates
TLS, redirects HTTP to HTTPS, adds HSTS, content-type, framing, referrer, and
browser-permission controls, and sends only `/api/*` to Spring Boot. Frontend,
backend, and PostgreSQL remain on internal Docker networks without host ports;
Actuator health endpoints are not routed publicly. The local Compose override
uses an internal CA only for loopback verification and must never be deployed.

Production never seeds demo users. Initial access is provisioned by an explicit
one-shot container command that reads a 16–128 character password from a
read-only file mount. Bootstrap is available only under the `prod` profile when
explicitly enabled and only while `app_users` is empty; it cannot overwrite or
add later users. Password contents are never accepted as command-line arguments
or logged. Normal backend containers leave bootstrap disabled.

## Public portfolio policy

- Use fictional company and person names.
- Use a hand-written Japanese specification dataset.
- Store only synthetic sample files in the public repository.
- Document security decisions without publishing credentials or infrastructure secrets.
