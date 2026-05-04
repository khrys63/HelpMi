-- User ↔ Organization : many-to-many
CREATE TABLE IF NOT EXISTS user_organizations (
    user_id         UUID NOT NULL,
    organization_id UUID NOT NULL,
    PRIMARY KEY (user_id, organization_id),
    CONSTRAINT fk_uo_user FOREIGN KEY (user_id)         REFERENCES users(id)         ON DELETE CASCADE,
    CONSTRAINT fk_uo_org  FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Migrate existing single-org data (INSERT IGNORE handles partial re-run)
INSERT IGNORE INTO user_organizations (user_id, organization_id)
SELECT id, organization_id FROM users WHERE organization_id IS NOT NULL;

-- Drop the FK on users.organization_id (auto-named users_ibfk_1 by MariaDB in V8)
-- IF EXISTS avoids error on re-run if already dropped
ALTER TABLE users DROP FOREIGN KEY IF EXISTS users_ibfk_1;

-- Drop the old single-org column
ALTER TABLE users DROP COLUMN IF EXISTS organization_id;

-- Ticket ↔ Organization : many-to-many
CREATE TABLE IF NOT EXISTS ticket_organizations (
    ticket_id       UUID NOT NULL,
    organization_id UUID NOT NULL,
    PRIMARY KEY (ticket_id, organization_id),
    CONSTRAINT fk_to_ticket FOREIGN KEY (ticket_id)       REFERENCES tickets(id)       ON DELETE CASCADE,
    CONSTRAINT fk_to_org    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Remove legacy tables
DROP TABLE IF EXISTS ticket_clients;
DROP TABLE IF EXISTS clients;
