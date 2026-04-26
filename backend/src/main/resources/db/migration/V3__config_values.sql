CREATE TABLE config_values (
    id UUID PRIMARY KEY DEFAULT (UUID()),
    category VARCHAR(20) NOT NULL,
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    color VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT true,
    position INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_config_value UNIQUE (category, code)
);

INSERT INTO config_values (category, code, label, color, position) VALUES
    ('STATUS', 'OPEN',        'Ouvert',    'blue',   1),
    ('STATUS', 'IN_PROGRESS', 'En cours',  'yellow', 2),
    ('STATUS', 'RESOLVED',    'Résolu',    'green',  3),
    ('STATUS', 'CLOSED',      'Fermé',     'gray',   4),
    ('STATUS', 'CANCELLED',   'Annulé',    'red',    5);

INSERT INTO config_values (category, code, label, color, position) VALUES
    ('PRIORITY', 'LOW',      'Faible',   'gray',   1),
    ('PRIORITY', 'MEDIUM',   'Moyenne',  'blue',   2),
    ('PRIORITY', 'HIGH',     'Haute',    'orange', 3),
    ('PRIORITY', 'CRITICAL', 'Critique', 'red',    4);

INSERT INTO config_values (category, code, label, color, position) VALUES
    ('TYPE', 'BUG',     'Bug',     'red',    1),
    ('TYPE', 'FEATURE', 'Feature', 'purple', 2),
    ('TYPE', 'TASK',    'Tâche',   'blue',   3),
    ('TYPE', 'SUPPORT', 'Support', 'green',  4);

INSERT INTO config_values (category, code, label, color, position) VALUES
    ('LINK_TYPE', 'RELATES_TO', 'Lié à',      'gray',   1),
    ('LINK_TYPE', 'DEPENDS_ON', 'Dépend de',  'blue',   2),
    ('LINK_TYPE', 'BLOCKS',     'Bloque',      'orange', 3),
    ('LINK_TYPE', 'DUPLICATES', 'Doublon de', 'red',    4);
