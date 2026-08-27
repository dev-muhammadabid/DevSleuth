CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_user_id  BIGINT NOT NULL UNIQUE,
    username        VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    avatar_url      VARCHAR(512),
    access_token    VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE github_connections (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    installation_id  BIGINT NOT NULL,
    access_token     VARCHAR(512),
    token_expires_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ
);

CREATE INDEX idx_github_connections_user ON github_connections(user_id);
CREATE INDEX idx_github_connections_installation ON github_connections(installation_id);
