# DevSleuth

![DevSleuth](images/devsleuth.png)

**AI-assisted hybrid code-review platform.** DevSleuth combines deterministic static analysis (Semgrep, SpotBugs, Checkstyle) with contextual LLM-based analysis to automatically review GitHub Pull Requests. Findings from both engines are normalized into a common model, deduplicated with semantic similarity, correlated, and scored by severity and confidence before landing in a review dashboard.

---

## Table of Contents

- [Why DevSleuth](#why-devsleuth)
- [How It Works](#how-it-works)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Local Development](#local-development)
- [Environment Variables](#environment-variables)
- [Finding Model](#finding-model)
- [Experiment Modes](#experiment-modes)
- [API](#api)
- [Documentation](#documentation)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [V1 Scope](#v1-scope)

---

## Why DevSleuth

Static analyzers are precise but noisy and context-blind. LLMs are context-aware but non-deterministic and prone to hallucination. DevSleuth runs both, then reconciles their output:

- **Static engine** catches known bug patterns, security sinks, and style violations deterministically.
- **AI engine** reasons about intent, cross-file context, and issues that rules can't express.
- **Hybrid reconciliation** deduplicates overlapping findings, marks agreement between engines as higher-confidence (`HYBRID`), and ranks the result so reviewers see what matters first.

## How It Works

Each analysis runs as an asynchronous review through a staged pipeline. The `ReviewStatus` reflects the current stage:

```
QUEUED → FETCHING → STATIC_ANALYSIS → AI_ANALYSIS → NORMALIZING → DEDUPLICATING → COMPLETED
                                                                                 ↘ FAILED
```

| Stage | What happens |
|-------|--------------|
| `QUEUED` | Review created, waiting for an executor thread |
| `FETCHING` | PR diff extracted from GitHub (with retry); an LLM PR summary is generated in the background |
| `STATIC_ANALYSIS` | Semgrep + SpotBugs + Checkstyle run against changed files |
| `AI_ANALYSIS` | LLM analyzes the diff with review context |
| `NORMALIZING` | Raw findings mapped to the common model; severity engine applied |
| `DEDUPLICATING` | Hybrid engine deduplicates, correlates, and ranks; AI fix suggestions generated in the background |
| `COMPLETED` / `FAILED` | Findings persisted with per-stage timings, or an error code recorded |

Static-only and AI-only stages are skipped when running in a single-engine [experiment mode](#experiment-modes).

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.3.2, Java 17 |
| Persistence | PostgreSQL, Spring Data JPA, Flyway migrations |
| Security | Spring Security, session-cookie auth, encrypted token columns |
| Frontend | Next.js 14.2.5, React 18, TypeScript 5.5 |
| Static analysis | Semgrep, SpotBugs, Checkstyle |
| Testing | JUnit + jqwik (backend), Vitest + Testing Library + fast-check (frontend) |

## Architecture

Modular monolith:

- **backend/** — Spring Boot REST API, GitHub integration, analysis orchestration
- **frontend/** — Next.js review dashboard
- **docs/** — Architecture and API documentation
- **test-corpus/** — Vulnerable Java examples with ground truth for evaluation

See [docs/architecture.md](docs/architecture.md) for the system diagram, package breakdown, and design decisions.

## Local Development

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL (running locally)
- A GitHub OAuth App (for authentication) and, for webhooks/API access, a GitHub App
- [Semgrep](https://semgrep.dev/docs/getting-started/) installed on `PATH` (or set `STATIC_ANALYZER_PATH`) for the static engine

### 1. Create the Database

Open `psql` and run:

```sql
CREATE USER devsleuth WITH PASSWORD 'changeme';
CREATE DATABASE devsleuth OWNER devsleuth;
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your database, GitHub OAuth/App credentials, and AI API key
```

The backend auto-loads `.env` on startup; real OS environment variables override it. `.env` is git-ignored — never commit real secrets. See [Environment Variables](#environment-variables).

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
5. Click **Analyze**
6. Watch the progress through the [pipeline stages](#how-it-works)
7. Inspect findings, severity/confidence scores, and AI fix suggestions

## Environment Variables

Configured in `.env` (see [.env.example](.env.example)):

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/devsleuth` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | PostgreSQL credentials |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | GitHub OAuth App (user login) |
| `GITHUB_APP_ID` / `GITHUB_APP_PRIVATE_KEY_PATH` / `GITHUB_WEBHOOK_SECRET` | GitHub App (webhooks + API access) |
| `AI_API_KEY` / `AI_PROVIDER` | LLM credentials and provider (default `openai`) |
| `STATIC_ANALYZER_PATH` | Path to the Semgrep binary |
| `ENCRYPTION_KEY` | Key for encrypting sensitive columns at rest (GitHub tokens) |
| `SESSION_COOKIE_SECURE` | Set `true` when serving over HTTPS |
| `SERVER_PORT` | Backend port (default `8080`) |
| `NEXT_PUBLIC_API_URL` | Backend URL the frontend calls |

## Finding Model

All findings from both engines are normalized into one common shape, then scored. See [docs/finding-model.md](docs/finding-model.md) for the full contract.

- **Source** — `STATIC`, `AI`, or `HYBRID` (both engines agreed)
- **Category** — `BUG`, `SECURITY`, `PERFORMANCE`, `QUALITY`
- **Severity** — `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`

Findings also carry a confidence score, file/line location, description, and optional AI-generated fix suggestion.

## Experiment Modes

To evaluate the value of hybrid review, a single PR can be analyzed under three modes:

| Mode | Runs |
|------|------|
| `STATIC_ONLY` | Static analyzers only |
| `AI_ONLY` | LLM analysis only |
| `HYBRID` | Both, with reconciliation (default) |

Comparison metrics are exposed in the experiments dashboard. See [docs/experiments.md](docs/experiments.md).

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

## Running Tests

Backend (JUnit + jqwik property tests):

```bash
cd backend
mvn test
```

Frontend (Vitest + Testing Library + fast-check):

```bash
cd frontend
npm test
```

## Project Structure

```
devsleuth/
├── backend/              Spring Boot application
│   └── src/main/java/com/devsleuth/
│       ├── auth/         GitHub OAuth, User entity/service
│       ├── github/       Webhook + API client (controller, service, entity, repository)
│       ├── repository/   Repository management
│       ├── pullrequest/  PR management
│       ├── review/       Orchestrator, HybridEngine, SeverityEngine, FindingNormalizer, timing
│       ├── analysis/     Static analyzers + parsers, AI pipeline, diff extraction
│       │   ├── staticanalysis/  Semgrep/SpotBugs/Checkstyle adapters, process isolation
│       │   └── ai/              Prompt/response/validation services, LLM client, fix + PR summary
│       ├── finding/      Finding entity, service, controller
│       ├── dashboard/    Summary stats
│       ├── experiment/   Experiment runner, modes, metrics
│       ├── common/       Shared enums, exceptions, base entity, security helpers
│       └── config/       Security, CORS, async, rate limiting, headers, session auth, dotenv
├── frontend/             Next.js application
│   └── src/
│       ├── app/          Pages (dashboard, repositories, pull-requests, reviews, experiments, auth, login)
│       ├── components/   Shared UI (badges, layout, filters, code viewer)
│       ├── hooks/        useApi, usePolling
│       ├── lib/          Typed API client
│       └── types/        TypeScript interfaces
├── test-corpus/          Vulnerable Java examples grouped by bugs/security/performance/quality
│   └── GROUND_TRUTH.md   Expected findings for evaluation
├── docs/                 Documentation
├── images/               Assets
└── .env.example          Environment variable template
```

## V1 Scope

Java repositories, GitHub PRs, Semgrep + SpotBugs + Checkstyle + LLM analysis, finding deduplication with semantic similarity, severity/confidence scoring, Next.js dashboard, and experiment modes. See [docs/V1_SCOPE.md](docs/V1_SCOPE.md) for what's in and out of V1.
