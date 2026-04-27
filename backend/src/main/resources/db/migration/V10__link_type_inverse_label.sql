ALTER TABLE config_values ADD COLUMN inverse_label VARCHAR(100);

UPDATE config_values SET inverse_label = 'Lié à'        WHERE category = 'LINK_TYPE' AND code = 'RELATES_TO';
UPDATE config_values SET inverse_label = 'Requis par'   WHERE category = 'LINK_TYPE' AND code = 'DEPENDS_ON';
UPDATE config_values SET inverse_label = 'Bloqué par'   WHERE category = 'LINK_TYPE' AND code = 'BLOCKS';
UPDATE config_values SET inverse_label = 'Dupliqué par' WHERE category = 'LINK_TYPE' AND code = 'DUPLICATES';
