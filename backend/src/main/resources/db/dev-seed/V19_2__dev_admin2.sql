INSERT IGNORE INTO users (id, keycloak_id, email, first_name, last_name, role) VALUES
    ('00000000-0000-0000-0000-000000000004', 'dev-admin2', 'admin2@helpmi.local', 'Admin', 'Two', 'ADMIN');

INSERT IGNORE INTO user_organizations (user_id, organization_id) VALUES
    ('00000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001');

INSERT IGNORE INTO user_projects (id, user_id, project_id, role) VALUES
    (UUID(), '00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'MEMBER');
