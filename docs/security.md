# DevSleuth Security

## Authentication & Authorization

- GitHub OAuth for user login (session-based)
- Webhook endpoints verified via HMAC-SHA256 signature
- API endpoints require authenticated session (ponytail: upgrade to proper RBAC later)

## Input Validation

- Webhook signature verification before processing payload
- Rate limiting: 60 req/min per IP (120 for webhooks)
- All external input (GitHub payloads, diff content) treated as untrusted

## Static Analysis Security

- Analyzers run via `ProcessIsolation`:
  - Fixed executable path (never user-provided)
  - No shell expansion (ProcessBuilder, not `Runtime.exec(String)`)
  - Environment cleared (only PATH + safe HOME)
  - Timeout enforcement (120s default)
  - Restricted working directory (temp dirs)
- Never execute arbitrary repository commands

## AI Safety Boundaries

- `AiInputSanitizer` applied before every LLM call:
  - Content length limits (200K chars total, 30K per file, 20 files max)
  - Prompt injection defense (5 regex patterns, wraps suspicious content in markers)
  - Secret redaction (12 patterns: AWS, GitHub, Slack, Stripe, PEM, JDBC, bearer, etc.)
- LLM output validated via `AiResponseValidator`:
  - JSON schema enforcement
  - Enum validation (category, severity)
  - Confidence range check [0, 1]
  - File path must exist in the PR
  - Line numbers must be positive
- Invalid responses retried (max 2 attempts), then dropped
- AI never executes code, accesses credentials, or modifies repositories

## Network Security

- CORS restricted to frontend origin (localhost:3000 for dev)
- Security headers: X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy
- HTTPS required for production (TLS via reverse proxy)

## Secret Management

- All secrets via environment variables (never in code)
- `.env` in .gitignore
- GitHub App private key stored as file path reference
