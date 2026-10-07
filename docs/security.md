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
4. Record the provider, model, purpose, document version, and requesting user.
5. Send only the minimum text required for the current operation.

The selected provider must have documented retention and training controls suitable for the deployment. Provider responses remain untrusted drafts until reviewed by a human.

The OpenAI adapter uses the Responses API with Structured Outputs, disables provider
storage (`store: false`), sends a per-job correlation ID, and reads the API key only
from the process environment. Project AI processing is disabled by default. The
backend strips email addresses and configured literal sensitive values before the
request, applies a character budget, and never stores or logs raw prompts.

## Logging rules

- Log identifiers and operation results, not full document text.
- Never log access tokens, passwords, signed URLs, or raw AI prompts containing customer data.
- Give every request and AI job a correlation ID.
- Keep security and audit logs separate from user-visible activity history.

## Public portfolio policy

- Use fictional company and person names.
- Use a hand-written Japanese specification dataset.
- Store only synthetic sample files in the public repository.
- Document security decisions without publishing credentials or infrastructure secrets.
