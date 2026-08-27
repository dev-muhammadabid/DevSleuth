CREATE TABLE experiments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    dataset     VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE experiment_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id   UUID NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    mode            VARCHAR(20) NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_experiment_runs_experiment ON experiment_runs(experiment_id);

CREATE TABLE experiment_metrics (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id            UUID NOT NULL REFERENCES experiment_runs(id) ON DELETE CASCADE,
    true_positives    INTEGER NOT NULL,
    false_positives   INTEGER NOT NULL,
    false_negatives   INTEGER NOT NULL,
    precision_score   DOUBLE PRECISION NOT NULL,
    recall_score      DOUBLE PRECISION NOT NULL,
    f1_score          DOUBLE PRECISION NOT NULL,
    analysis_time_ms  BIGINT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ
);

CREATE INDEX idx_experiment_metrics_run ON experiment_metrics(run_id);
