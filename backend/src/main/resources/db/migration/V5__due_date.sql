ALTER TABLE tickets ADD COLUMN due_date DATE;

INSERT INTO config_values (category, code, label, color, position) VALUES
    ('TYPE', 'PERIODIQUE', 'Périodique', 'teal', 5);
