CREATE TABLE ticket_watchers (
    ticket_id UUID NOT NULL,
    user_id   UUID NOT NULL,
    PRIMARY KEY (ticket_id, user_id),
    CONSTRAINT fk_tw_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_tw_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE
);
