-- DevSleuth V1 Schema

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_user_id BIGINT NOT NULL UNIQUE,
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE github_connections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    installation_id BIGINT NOT NULL,
    access_token    VARCHAR(512),
    token_expires_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_github_connections_user ON github_connections(user_id);
CREATE INDEX idx_github_connections_installation ON github_connections(installation_id);

CREATE TABLE repositories (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_repository_id BIGINT NOT NULL UNIQUE,
    owner                VARCHAR(255) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    full_name            VARCHAR(512) NOT NULL,
    default_branch       VARCHAR(255),
    language             VARCHAR(100),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ
);

CREATE INDEX idx_repositories_full_name ON repositories(full_name);

CREATE TABLE pull_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    github_pr_id    BIGINT NOT NULL,
    number          INTEGER NOT NULL,
    title           VARCHAR(512) NOT NULL,
    author          VARCHAR(255),
    source_branch   VARCHAR(255),
    target_branch   VARCHAR(255),
    commit_sha      VARCHAR(40),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_pull_requests_repo ON pull_requests(repository_id);
CREATE UNIQUE INDEX idx_pull_requests_repo_number ON pull_requests(repository_id, number);

CREATE TABLE reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pull_request_id     UUID NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    commit_sha          VARCHAR(40) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    duration_ms         BIGINT,
    static_finding_count INTEGER,
    ai_finding_count    INTEGER,
    final_finding_count INTEGER,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ
);

CREATE INDEX idx_reviews_pr ON reviews(pull_request_id);

CREATE TABLE findings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id       UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    source          VARCHAR(20) NOT NULL,
    category        VARCHAR(20) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    confidence      INTEGER NOT NULL CHECK (confidence BETWEEN 0 AND 100),
    title           VARCHAR(512) NOT NULL,
    description     TEXT,
    recommendation  TEXT,
    file_path       VARCHAR(1024) NOT NULL,
    line_start      INTEGER,
    line_end        INTEGER,
    fingerprint     VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_findings_review ON findings(review_id);
CREATE INDEX idx_findings_fingerprint ON findings(review_id, fingerprint);
CREATE INDEX idx_findings_severity ON findings(review_id, severity);

CREATE TABLE experiments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    variant     VARCHAR(255),
    metadata    TEXT,
    review_id   UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE INDEX idx_experiments_review ON experiments(review_id);
