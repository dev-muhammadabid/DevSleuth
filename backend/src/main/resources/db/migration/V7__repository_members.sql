-- Move from single-owner repositories to shared membership: multiple DevSleuth users
-- (anyone with GitHub access who syncs the repo) can see and act on the same repository.

CREATE TABLE repository_members (
    repository_id UUID NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (repository_id, user_id)
);

CREATE INDEX idx_repository_members_user ON repository_members(user_id);

-- Backfill existing single-owner links as the first members.
INSERT INTO repository_members (repository_id, user_id)
SELECT id, user_id FROM repositories WHERE user_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Ownership is now expressed only via repository_members.
DROP INDEX IF EXISTS idx_repositories_user;
ALTER TABLE repositories DROP COLUMN user_id;
