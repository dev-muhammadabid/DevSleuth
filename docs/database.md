# DevSleuth Database Schema

PostgreSQL 16. Managed via Flyway migrations (`backend/src/main/resources/db/migration/`).

## Entity Relationship

```
users
  |
  +-- github_connections (1:N)
  +-- repositories (1:N, via user_id)
        |
        +-- pull_requests (1:N)
              |
              +-- reviews (1:N)
                    |
                    +-- findings (1:N)

experiments
  |
  +-- experiment_runs (1:N)
        |
        +-- experiment_metrics (1:1)
```

## Tables

### users
| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| github_user_id | BIGINT UNIQUE | GitHub user ID |
| username | VARCHAR(255) | |
| email | VARCHAR(255) | nullable |
| avatar_url | VARCHAR(512) | |
| access_token | VARCHAR(512) | GitHub OAuth token |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### repositories
| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| github_repository_id | BIGINT UNIQUE | |
| owner | VARCHAR(255) | |
| name | VARCHAR(255) | |
| full_name | VARCHAR(512) | e.g. `owner/name` |
| default_branch | VARCHAR(255) | |
| language | VARCHAR(100) | |
| connected | BOOLEAN | Whether analysis is active |
| user_id | UUID FK → users | |

### pull_requests
| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| repository_id | UUID FK | |
| github_pr_id | BIGINT | |
| number | INTEGER | PR number |
| title | VARCHAR(512) | |
| author | VARCHAR(255) | |
| source_branch | VARCHAR(255) | |
| target_branch | VARCHAR(255) | |
| commit_sha | VARCHAR(40) | HEAD SHA |
| status | VARCHAR(20) | OPEN/CLOSED/MERGED |

### reviews
| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| pull_request_id | UUID FK | |
| commit_sha | VARCHAR(40) | |
| status | VARCHAR(20) | State machine: QUEUED→...→COMPLETED/FAILED |
| started_at | TIMESTAMPTZ | |
| completed_at | TIMESTAMPTZ | |
| duration_ms | BIGINT | |
| static_finding_count | INTEGER | |
| ai_finding_count | INTEGER | |
| final_finding_count | INTEGER | After dedup |
| error_message | TEXT | On FAILED |

### findings
| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| review_id | UUID FK | |
| source | VARCHAR(20) | STATIC/AI/HYBRID |
| category | VARCHAR(20) | BUG/SECURITY/PERFORMANCE/QUALITY |
| severity | VARCHAR(20) | CRITICAL/HIGH/MEDIUM/LOW/INFO |
| confidence | INTEGER | 0-100 |
| title | VARCHAR(512) | |
| description | TEXT | |
| recommendation | TEXT | |
| file_path | VARCHAR(1024) | |
| line_start | INTEGER | |
| line_end | INTEGER | |
| fingerprint | VARCHAR(64) | SHA-256 based, for dedup |

### experiments / experiment_runs / experiment_metrics
See V6 migration for full schema.

## Migrations

| Version | Description |
|---------|-------------|
| V1 | users + github_connections |
| V2 | repositories |
| V3 | pull_requests |
| V4 | reviews |
| V5 | findings + experiments (old) |
| V6 | experiments + experiment_runs + experiment_metrics |
