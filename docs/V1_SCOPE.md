# DevSleuth V1 Scope

## In Scope

- **GitHub Integration**: GitHub App with webhook receiver, installation token auth
- **Java Repositories**: V1 targets Java projects only
- **Pull Request Detection**: Listen for PR opened/synchronized events
- **PR Diff Extraction**: Fetch changed files and diffs via GitHub API
- **Static Analysis**: Semgrep (primary), SpotBugs (Java bytecode)
- **AI Analysis**: OpenAI/Anthropic API for contextual code review
- **Finding Normalization**: Common format (category, severity, confidence, file, line, title, description, recommendation)
- **Finding Deduplication**: Fingerprint-based merging of overlapping findings
- **Severity & Confidence Scoring**: Deterministic for static tools, heuristic for AI
- **Recommendations**: Each finding includes a fix suggestion
- **Review History**: Track every analysis run per PR
- **Dashboard**: Next.js UI showing PRs, findings, filtering
- **PostgreSQL Persistence**: All entities stored relationally
- **Experimental Metrics**: Basic tracking of prompt strategies and analyzer effectiveness

## Out of Scope (V1)

- Mobile application
- VS Code extension
- Multi-language support (beyond Java)
- Autonomous code fixing / auto-apply patches
- Organization billing / subscription management
- Marketplace for custom rules
- Microservices / Kubernetes / distributed infrastructure
- SonarQube integration (heavy; Semgrep + SpotBugs covers V1)
- Message queue (RabbitMQ/Kafka) — async via Spring @Async is sufficient for V1

## Architecture Decision

Modular monolith. Single Spring Boot deployable with logical package boundaries.
Split into services later only if load demands it.

## Deployment (V1)

- Docker Compose (backend + frontend + PostgreSQL)
- No orchestrator needed at this stage
