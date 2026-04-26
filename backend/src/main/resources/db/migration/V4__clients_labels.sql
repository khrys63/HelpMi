CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE labels (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(100) NOT NULL,
    color VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT labels_name_unique UNIQUE (name)
);

CREATE TABLE ticket_clients (
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    PRIMARY KEY (ticket_id, client_id)
);

CREATE TABLE ticket_labels (
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    label_id UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (ticket_id, label_id)
);
