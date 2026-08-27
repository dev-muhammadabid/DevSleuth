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
