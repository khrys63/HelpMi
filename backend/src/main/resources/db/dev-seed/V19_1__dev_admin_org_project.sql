-- Add admin to demo organization and demo project
INSERT IGNORE INTO user_organizations (user_id, organization_id) VALUES
    ('00000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001');

INSERT IGNORE INTO user_projects (id, user_id, project_id, role) VALUES
    (UUID(), '00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'MEMBER');
