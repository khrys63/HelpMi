UPDATE config_values SET code = 'ANNUEL', label = 'Annuel' WHERE category = 'TYPE' AND code = 'PERIODIQUE';
UPDATE tickets SET type = 'ANNUEL' WHERE type = 'PERIODIQUE';

INSERT INTO config_values (category, code, label, color, position) VALUES
    ('TYPE', 'MENSUEL',     'Mensuel',     'cyan',   6),
    ('TYPE', 'TRIMESTRIEL', 'Trimestriel', 'violet', 7);
