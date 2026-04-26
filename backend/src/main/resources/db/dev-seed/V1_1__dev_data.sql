INSERT IGNORE INTO users (id, keycloak_id, email, first_name, last_name, role) VALUES
    ('00000000-0000-0000-0000-000000000001', 'dev-admin',  'admin@helpmi.local',  'Admin',  'Dev', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000002', 'dev-agent',  'agent@helpmi.local',  'Agent',  'Dev', 'AGENT'),
    ('00000000-0000-0000-0000-000000000003', 'dev-client', 'client@helpmi.local', 'Client', 'Dev', 'CLIENT');

INSERT IGNORE INTO projects (id, name, `key`, description, created_by) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Demo Project', 'DEMO', 'Projet de démonstration', '00000000-0000-0000-0000-000000000001');
