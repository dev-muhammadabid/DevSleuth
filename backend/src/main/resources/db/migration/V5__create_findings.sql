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
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_findings_review ON findings(review_id);
CREATE INDEX idx_findings_fingerprint ON findings(review_id, fingerprint);
CREATE INDEX idx_findings_severity ON findings(review_id, severity);
