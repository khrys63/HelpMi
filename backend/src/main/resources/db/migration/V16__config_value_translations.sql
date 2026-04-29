ALTER TABLE config_values
    ADD COLUMN label_en    VARCHAR(100),
    ADD COLUMN label_bg    VARCHAR(100),
    ADD COLUMN inverse_label_en VARCHAR(100),
    ADD COLUMN inverse_label_bg VARCHAR(100);
