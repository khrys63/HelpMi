CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT (UUID()),
    action      VARCHAR(100) NOT NULL,
    actor_id    UUID,
    actor_email VARCHAR(255),
    target_type VARCHAR(50),
    target_id   VARCHAR(255),
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_actor_id   ON audit_log (actor_id);
CREATE INDEX idx_audit_log_action     ON audit_log (action);
