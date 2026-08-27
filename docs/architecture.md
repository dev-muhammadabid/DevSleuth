# DevSleuth Architecture

## Overview

DevSleuth is a modular monolith: a single Spring Boot deployable with logical package boundaries, paired with a Next.js frontend.

## System Diagram

```
                         GitHub
                           |
                           | Pull Request event
                           v
                  +------------------+
                  | GitHub Webhook   |
                  | Controller       |
                  +--------+---------+
                           |
                           v
                  +------------------+
                  | GitHubWebhook    |
                  | Service          |
                  +--------+---------+
                           | (async)
                           v
                  +------------------+
                  | PullRequest      |
                  | Service          |
                  +--------+---------+
                           |
                           v
                  +------------------+
                  | Review           |
                  | Orchestrator     |
                  +--------+---------+
                           |
              +------------+------------+
              |                         |
              v                         v
      +---------------+         +---------------+
      | Static        |         | AI Analysis   |
      | Analysis      |         | Service       |
      | Engine        |         | (LLM)        |
      +-------+-------+         +-------+-------+
              |                         |
              +------------+------------+
                           |
                           v
                  +------------------+
                  | Finding          |
                  | Normalizer       |
                  +--------+---------+
                           |
                           v
                  +------------------+
                  | Severity         |
                  | Engine           |
                  +--------+---------+
                           |
                           v
                  +------------------+
                  | Hybrid Engine    |
                  | (Dedup+Rank)     |
                  +--------+---------+
                           |
                           v
                  +------------------+
                  | PostgreSQL       |
                  +------------------+
                           |
                           v
                  +------------------+
                  | Next.js          |
                  | Frontend         |
                  +------------------+
```

## Backend Packages

```
com.devsleuth
├── auth           — GitHub OAuth, User entity, session management
├── github         — Webhook controller/service, GitHub API client, repo/PR fetchers
├── repository     — Repository entity, service, controller
├── pullrequest    — PullRequest entity, service, controller
├── review         — Review entity, ReviewOrchestrator, HybridEngine, SeverityEngine, timers
├── analysis
│   ├── ai         — LLM integration (prompt, parser, validator, sanitizer, context builder)
│   ├── model      — AnalysisInput, RawFinding (shared between static + AI)
│   ├── service    — DiffExtractionService
│   └── staticanalysis — StaticAnalyzer interface, adapters (Semgrep/SpotBugs/Checkstyle), parsers
├── finding        — Finding entity, service, controller
├── dashboard      — Dashboard controller, summary DTO
├── experiment     — ExperimentMode, ExperimentRunner, persistence, controller
├── common
│   ├── entity     — BaseEntity
│   ├── enums      — Severity, FindingCategory, FindingSource, ReviewStatus, PullRequestStatus
│   └── exception  — ReviewException hierarchy, RetryStrategy
└── config         — SecurityConfig, CorsConfig, AsyncConfig, GitHubProperties, rate limiting
```

## Frontend Structure

```
frontend/src/
├── app/             — Next.js App Router pages
├── components/      — Shared UI components (badges, layout, filters, code viewer)
├── hooks/           — useApi, usePolling
├── lib/             — Typed API client
├── types/           — TypeScript interfaces
└── styles/          — Global CSS
```

## Key Design Decisions

1. **Modular monolith** — split into services later only if load demands it
2. **Async pipeline** — webhook returns 200 immediately, analysis runs on thread pool
3. **Adapter pattern** for static analyzers — adding a new tool = one class
4. **Hybrid dedup** — Jaccard similarity on tokenized titles + fingerprint-based exact match
5. **Severity policy** — LLM does not control severity alone; policy engine enforces rules
6. **Experiment modes** — compare STATIC_ONLY vs AI_ONLY vs HYBRID on same dataset
