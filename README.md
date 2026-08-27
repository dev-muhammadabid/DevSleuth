# DevSleuth

AI-assisted hybrid code-review platform. Combines deterministic static analysis (Semgrep, SpotBugs) with contextual LLM-based analysis to automatically review GitHub Pull Requests.

## Architecture

Modular monolith:
- **backend/** — Spring Boot (Java 17), REST API, GitHub integration, analysis orchestration
- **frontend/** — Next.js (TypeScript), review dashboard
- **database/** — Flyway migrations for PostgreSQL
- **docs/** — Architecture and scope documentation

## Quick Start

```bash
# Prerequisites: Java 17+, Node 18+, Docker, PostgreSQL

# Copy environment config
cp .env.example .env

# Start infrastructure
docker-compose up -d postgres

# Run backend
cd backend
mvn spring-boot:run

# Run frontend
cd frontend
npm install
npm run dev
```

## V1 Scope

Java repositories, GitHub PRs, Semgrep + SpotBugs + LLM analysis, finding deduplication, severity/confidence scoring, Next.js dashboard.

See [docs/V1_SCOPE.md](docs/V1_SCOPE.md) for full scope definition.
