# DevSleuth

AI-assisted hybrid code-review platform. Combines deterministic static analysis (Semgrep, SpotBugs, Checkstyle) with contextual LLM-based analysis to automatically review GitHub Pull Requests.

## Architecture

Modular monolith:
- **backend/** — Spring Boot (Java 17), REST API, GitHub integration, analysis orchestration
- **frontend/** — Next.js (TypeScript), review dashboard
- **docs/** — Architecture and API documentation
- **test-corpus/** — Vulnerable Java examples for evaluation

## Local Development

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL (running locally)
- A GitHub OAuth App (for authentication)

### 1. Create the Database

Open `psql` and run:

```sql
CREATE USER devsleuth WITH PASSWORD 'changeme';
CREATE DATABASE devsleuth OWNER devsleuth;
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your GitHub OAuth credentials and (optionally) AI API key
```

### 3. Start Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs at http://localhost:8080. Flyway creates all tables automatically on first start.

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at http://localhost:3000

### 5. Use DevSleuth

1. Open http://localhost:3000
2. Sign in with GitHub
3. Connect a repository
4. Select a Pull Request
5. Click "Analyze"
6. Watch the progress (QUEUED → FETCHING → STATIC → AI → COMPLETED)
7. Inspect findings

## API

See [docs/api.md](docs/api.md) for full API documentation.

## Documentation

| Document | Description |
|----------|-------------|
| [architecture.md](docs/architecture.md) | System diagram, packages, design decisions |
| [database.md](docs/database.md) | Schema, tables, migrations |
| [api.md](docs/api.md) | All REST endpoints |
| [github-integration.md](docs/github-integration.md) | OAuth, webhooks, API usage |
| [static-analysis.md](docs/static-analysis.md) | Analyzer adapters, parsers |
| [ai-analysis.md](docs/ai-analysis.md) | LLM pipeline, prompts, safety |
| [finding-model.md](docs/finding-model.md) | Common finding format, severity policy |
| [security.md](docs/security.md) | Auth, input validation, process isolation |
| [testing.md](docs/testing.md) | Unit tests, test corpus |
| [experiments.md](docs/experiments.md) | STATIC_ONLY vs AI_ONLY vs HYBRID |
| [V1_SCOPE.md](docs/V1_SCOPE.md) | What's in/out of V1 |

## V1 Scope

Java repositories, GitHub PRs, Semgrep + SpotBugs + Checkstyle + LLM analysis, finding deduplication with semantic similarity, severity/confidence scoring, Next.js dashboard, experiment modes.

## Running Tests

```bash
cd backend
mvn test
```

## Project Structure

```
devsleuth/
├── backend/              Spring Boot application
│   └── src/main/java/com/devsleuth/
│       ├── auth/         GitHub OAuth
│       ├── github/       Webhook, API client
│       ├── repository/   Repository management
│       ├── pullrequest/  PR management
│       ├── review/       Orchestrator, HybridEngine, SeverityEngine
│       ├── analysis/     Static analyzers, AI engine, diff extraction
│       ├── finding/      Finding entity, service, controller
│       ├── dashboard/    Summary stats
│       ├── experiment/   Experiment runner, metrics
│       ├── common/       Shared enums, exceptions, base entity
│       └── config/       Security, CORS, async, properties
├── frontend/             Next.js application
│   └── src/
│       ├── app/          Pages (dashboard, repos, PRs, reviews, experiments)
│       ├── components/   Shared UI (badges, layout, filters, code viewer)
│       ├── hooks/        useApi, usePolling
│       ├── lib/          Typed API client
│       └── types/        TypeScript interfaces
├── test-corpus/          Vulnerable Java examples with ground truth
├── docs/                 Documentation
└── .env.example          Environment variable template
```
