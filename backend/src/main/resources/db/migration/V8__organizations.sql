CREATE TABLE organizations (
    id           UUID         NOT NULL PRIMARY KEY DEFAULT (UUID()),
    name         VARCHAR(100) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

ALTER TABLE users ADD COLUMN organization_id UUID NULL REFERENCES organizations(id);

CREATE TABLE organization_projects (
    organization_id UUID NOT NULL REFERENCES organizations(id),
    project_id      UUID NOT NULL REFERENCES projects(id),
    PRIMARY KEY (organization_id, project_id)
);
