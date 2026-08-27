-- Add user ownership and structured JSONB storage to experiments, and add run
-- lifecycle tracking (status + error message) to experiment_runs.

-- experiments: owner, structured dataset, and ground truth.
ALTER TABLE experiments ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE experiments ALTER COLUMN dataset TYPE JSONB USING dataset::jsonb;
ALTER TABLE experiments ADD COLUMN ground_truth JSONB;

CREATE INDEX idx_experiments_user ON experiments(user_id);

-- experiment_runs: status polled by the UI, error message on failure.
ALTER TABLE experiment_runs ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'RUNNING';
ALTER TABLE experiment_runs ADD COLUMN error_message TEXT;
