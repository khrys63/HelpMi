CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    keycloak_id VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL DEFAULT '',
    last_name VARCHAR(255) NOT NULL DEFAULT '',
    role VARCHAR(20) NOT NULL DEFAULT 'CLIENT',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_keycloak_id ON users(keycloak_id);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(255) NOT NULL,
    `key` VARCHAR(10) NOT NULL UNIQUE,
    description TEXT,
    ticket_sequence INTEGER NOT NULL DEFAULT 0,
    created_by UUID REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    reference VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    type VARCHAR(20) NOT NULL DEFAULT 'TASK',
    project_id UUID NOT NULL REFERENCES projects(id),
    reporter_id UUID REFERENCES users(id),
    assignee_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP
);

CREATE INDEX idx_tickets_project ON tickets(project_id);
CREATE INDEX idx_tickets_status ON tickets(project_id, status);
CREATE INDEX idx_tickets_assignee ON tickets(assignee_id);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id),
    body TEXT NOT NULL,
    edited BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    file_name VARCHAR(500) NOT NULL,
    stored_name VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(255),
    size BIGINT NOT NULL DEFAULT 0,
    uploaded_by UUID REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);
