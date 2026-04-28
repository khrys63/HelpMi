-- Rename project role codes: GESTIONNAIRE→MANAGER, UTILISATEUR→MEMBER
-- Labels stay French (UI display), codes become English (technical identifiers)

UPDATE user_projects SET role = 'MANAGER' WHERE role = 'GESTIONNAIRE';
UPDATE user_projects SET role = 'MEMBER'  WHERE role = 'UTILISATEUR';

ALTER TABLE user_projects ALTER COLUMN role SET DEFAULT 'MEMBER';

UPDATE config_values SET code = 'MANAGER', label = 'Gestionnaire'
    WHERE category = 'PROJECT_ROLE' AND code = 'GESTIONNAIRE';
UPDATE config_values SET code = 'MEMBER', label = 'Membre'
    WHERE category = 'PROJECT_ROLE' AND code = 'UTILISATEUR';
