CREATE TABLE ticket_history (
    id          UUID         NOT NULL DEFAULT (UUID())  PRIMARY KEY,
    ticket_id   UUID         NOT NULL,
    changed_by  UUID         NOT NULL,
    changed_at  TIMESTAMP(6) NOT NULL DEFAULT NOW(6),
    field       VARCHAR(50)  NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    CONSTRAINT fk_th_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_th_user   FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_th_ticket_date ON ticket_history (ticket_id, changed_at DESC);
