-- Décale les positions pour insérer STAND_BY entre IN_PROGRESS et RESOLVED
UPDATE config_values SET position = 6 WHERE category = 'STATUS' AND code = 'CANCELLED';
UPDATE config_values SET position = 5 WHERE category = 'STATUS' AND code = 'CLOSED';
UPDATE config_values SET position = 4 WHERE category = 'STATUS' AND code = 'RESOLVED';

INSERT INTO config_values (category, code, label, color, position)
VALUES ('STATUS', 'STAND_BY', 'Stand by', 'purple', 3);
