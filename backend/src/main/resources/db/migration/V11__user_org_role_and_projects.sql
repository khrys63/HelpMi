ALTER TABLE users ADD COLUMN organization_role VARCHAR(100) NULL;

CREATE TABLE user_projects (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, project_id)
);

INSERT INTO config_values (category, code, label, position) VALUES
    ('ORG_ROLE', 'ADMINISTRATEUR', 'Administrateur', 1),
    ('ORG_ROLE', 'GESTIONNAIRE',   'Gestionnaire',   2);
