ALTER TABLE task_group
    ADD COLUMN image_base64_de TEXT NULL,
    ADD COLUMN image_base64_en TEXT NULL,
    ADD COLUMN image_truncated BOOLEAN NOT NULL DEFAULT FALSE;
