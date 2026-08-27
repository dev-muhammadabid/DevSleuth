CREATE TABLE repositories (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_repository_id BIGINT NOT NULL UNIQUE,
    owner                VARCHAR(255) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    full_name            VARCHAR(512) NOT NULL,
    default_branch       VARCHAR(255),
    language             VARCHAR(100),
    connected            BOOLEAN NOT NULL DEFAULT false,
    user_id              UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ
);

CREATE INDEX idx_repositories_full_name ON repositories(full_name);
CREATE INDEX idx_repositories_user ON repositories(user_id);
