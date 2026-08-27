CREATE TABLE reviews (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pull_request_id      UUID NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    commit_sha           VARCHAR(40) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at           TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    duration_ms          BIGINT,
    static_finding_count INTEGER,
    ai_finding_count     INTEGER,
    final_finding_count  INTEGER,
    error_message        TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ
);

CREATE INDEX idx_reviews_pr ON reviews(pull_request_id);
