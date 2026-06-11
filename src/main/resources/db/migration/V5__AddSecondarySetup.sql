ALTER TABLE task_group
    ADD COLUMN secondary_setup_statements TEXT;

UPDATE task_group
SET secondary_setup_statements = setup_statements
WHERE secondary_setup_statements IS NULL;

ALTER TABLE task_group
    ALTER COLUMN secondary_setup_statements SET NOT NULL;
