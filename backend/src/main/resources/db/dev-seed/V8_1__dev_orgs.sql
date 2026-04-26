INSERT IGNORE INTO organizations (id, name, active) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Demo Organisation', true);

-- Assign agent and client dev users to the demo org; admin has no org (sees everything)
UPDATE users SET organization_id = '20000000-0000-0000-0000-000000000001'
WHERE id IN ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003');

INSERT IGNORE INTO organization_projects (organization_id, project_id) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001');
