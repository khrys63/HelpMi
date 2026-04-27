-- Recreate user_projects with UUID PK and role column
DROP TABLE IF EXISTS user_projects;
CREATE TABLE user_projects (
    id         UUID         NOT NULL PRIMARY KEY DEFAULT (UUID()),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    role       VARCHAR(100) NOT NULL DEFAULT 'UTILISATEUR',
    CONSTRAINT uq_user_project UNIQUE (user_id, project_id)
);

-- Migrate global roles
UPDATE users SET role = 'USER' WHERE role IN ('AGENT', 'CLIENT');

-- Remove org role (now carried by user_projects.role)
ALTER TABLE users DROP COLUMN IF EXISTS organization_role;

-- Replace ORG_ROLE config values with PROJECT_ROLE
DELETE FROM config_values WHERE category = 'ORG_ROLE';
INSERT INTO config_values (category, code, label, position) VALUES
    ('PROJECT_ROLE', 'GESTIONNAIRE', 'Gestionnaire', 1),
    ('PROJECT_ROLE', 'UTILISATEUR',  'Utilisateur',  2);
