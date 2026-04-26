CREATE TABLE personal_tokens (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_personal_tokens_user ON personal_tokens(user_id);
