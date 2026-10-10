# BridgeFlow v1.0.0 Release Notes

> **Initial Production-Ready Technical MVP Release**  
> *Release Target: v1.0.0 | Commit: CI-verified main branch | License: Proprietary / Portfolio Showcase*

---

## 1. Executive Summary

BridgeFlow v1.0.0 marks the completion of the core Japanese–Vietnamese bilingual requirements workspace. Designed specifically for offshore delivery teams and Bridge Software Engineers (BrSE), BridgeFlow bridges communication gaps, enforces strict human-in-the-loop governance over AI outputs, and maintains end-to-end traceability across specifications, requirements, criteria, and test cases.

This release includes a complete containerized HTTPS stack, automated offline AI quality benchmarking, comprehensive operational disaster recovery tools, and zero-cost on-demand public demonstration capabilities via Cloudflare Quick Tunnel.

---

## 2. Key Capabilities Delivered

### Core Product Capabilities (Milestones 1–6A)
- **Bilingual Requirement Management**: Japanese and Vietnamese title, description, and status management with server-side search, filtering, and pagination.
- **Document Ingestion & Versioning**: Multi-format document upload (txt, md, docx) with cryptographic SHA-256 verification and immutable version tracking.
- **Glossary Engine**: Project-scoped terminology store ensuring consistent translation of technical terms.
- **Human-in-the-Loop AI Extraction**: AI candidate requirements, clarification questions, and acceptance criteria originate strictly in `DRAFT` status and require human confirmation.
- **Downstream Test-Case Synthesis**: Test cases are generated exclusively from human-approved acceptance criteria, preventing speculative testing.
- **End-to-End Traceability & Impact Analysis**: Bidirectional traceability links across documents, requirements, criteria, and tests. Explainable change-impact reports identify affected artifacts upon specification updates.
- **Cross-Requirement Relations**: Audited dependency, duplicate, split, and merge relations.
- **Bilingual Project Exports**: Clean UTF-8 CSV and structured Markdown exports with audit logging.
- **Observability**: Correlation IDs (`X-Correlation-Id`), structured JSON logging, Prometheus metrics, and liveness/readiness probes.

### Operational Hardening & Production Operations (Milestones 7–9A)
- **Unified Single-Origin Gateway**: Caddy reverse proxy providing automated TLS termination, security headers (`HSTS`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`), and same-origin routing (`/` to frontend, `/api/*` to Spring Boot).
- **Network & User Isolation**: Spring Boot, PostgreSQL, and Node frontend run on private internal networks without exposed host ports. Containers run as non-privileged users with dropped Linux capabilities.
- **Token Hashing & RBAC**: Bearer authentication tokens stored only as SHA-256 hashes. Project-scoped RBAC (`ADMIN`, `OPERATOR`, `MEMBER`, `VIEWER`).
- **One-Shot Production Bootstrap**: Automated `bootstrap-production-user.ps1` script allowing secure initial operator creation; subsequent runs are safely rejected.
- **Automated Backup & Disaster Recovery**: Tested PowerShell runbooks (`backup-production.ps1` and `restore-production.ps1`) providing database dumps, document volume archiving, and manifest validation.

### Zero-Cost Public Demonstration (Milestone 9B-Free)
- **On-Demand Cloudflare Quick Tunnel**: Instant public HTTPS access (`*.trycloudflare.com`) via `scripts/start-demo-tunnel.ps1` without requiring domain purchase, cloud server rental, or credit cards.
- **Clean Teardown**: `scripts/stop-demo-tunnel.ps1 -RemoveData` guarantees zero leftover processes, files, or volumes.

### Deterministic Offline AI Evaluation (Milestone 10A)
- **Versioned Synthetic Corpus**: `eval/corpus/corpus-v1.json` (v1.0.0) containing 16 diverse Japanese–Vietnamese specification cases.
- **Automated Evaluation Harness**: `npm run eval:ai:offline` / `scripts\evaluate-ai-offline.cmd` benchmarking 12 capabilities:
  - Safety & Redaction Rate: **100.0%** (target >= 100%)
  - Human Review Enforcement: **100.0%** (target >= 100%)
  - Retry & Idempotency Rate: **100.0%** (target >= 100%)
  - Traceability Completeness: **100.0%** (target >= 100%)
  - Glossary Adherence: **100.0%** (target >= 100%)
  - Field Coverage: **100.0%** (target >= 90%)
  - Invalid / Duplicate Rate: **0.0%** (target <= 5%)

---

## 3. Verification & Quality Evidence

All release gates have been validated on the main branch:
- **Backend Integration Tests**: 28/28 tests passed (0 failures, 0 errors, 0 skipped) against disposable PostgreSQL 17 cluster.
- **Frontend Quality**: ESLint, TypeScript type-check, and Next.js / Vite production bundle passed.
- **Release Verification Gate**: `scripts\verify-release.cmd` exited with status `0`.
- **Infrastructure as Code**: Semantic validation of Docker Compose files and PowerShell AST validation of all 7 operational scripts passed.
- **Continuous Integration**: GitHub Actions CI workflow (Run #4 on commit `bd33fad`) passed across all 3 jobs:
  1. `Backend (Java 21 / PostgreSQL 17)`: Success
  2. `Frontend and production configuration`: Success
  3. `Production container images`: Success

---

## 4. How to Reproduce from Fresh Clone

### Prerequisites
- Windows 10/11 or Linux
- Git, Docker Desktop
- Node.js 22 LTS, OpenJDK 21, Maven 3.9+ (for local development)

### Quick Start: Zero-Cost On-Demand Demo
```powershell
git clone https://github.com/anhtrinh05/bridgeflow.git
cd bridgeflow

# Start isolated demo stack and obtain temporary public HTTPS URL
.\scripts\start-demo-tunnel.ps1

# Bootstrap demo operator account
.\scripts\bootstrap-production-user.ps1 -ProjectName bridgeflow-demo -EnvFile .tooling\demo-tunnel\.env.demo -Email operator@bridgeflow.local -DisplayName "Demo Operator"

# When finished, shut down and clean all data
.\scripts\stop-demo-tunnel.ps1 -RemoveData
```

### Run Release Gate & AI Evaluation
```powershell
# Run the complete release verification suite
.\scripts\verify-release.cmd

# Run offline AI evaluation standalone
npm run eval:ai:offline
```

---

## 5. Transparent Limitations & Deferred Roadmap

- **Ephemeral Tunnel**: The Cloudflare Quick Tunnel URL is temporary and intended for live portfolio demonstration. An always-on production deployment would require an approved VPS host (e.g., Hetzner CX23) and domain DNS records.
- **Deterministic AI Stub Provider**: Continuous integration uses the local deterministic stub provider to ensure 100% reproducible tests without billing cloud LLMs. Real cloud LLM providers (e.g., OpenAI gpt-4o-mini) can be configured via environment variables.
- **Single-Node Deployment**: The v1.0.0 release targets a single hardened host via Docker Compose. Multi-node Kubernetes clustering and distributed queues (Redis) are deferred to post-MVP milestones.
