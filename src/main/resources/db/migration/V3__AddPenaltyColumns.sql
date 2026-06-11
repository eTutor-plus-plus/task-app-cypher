ALTER TABLE task
    ADD COLUMN superfluous_columns_penalty NUMERIC(5, 2) NOT NULL DEFAULT -1,
    ADD COLUMN missing_rows_penalty        NUMERIC(5, 2) NOT NULL DEFAULT -1,
    ADD COLUMN superfluous_rows_penalty    NUMERIC(5, 2) NOT NULL DEFAULT -1,
    ADD COLUMN wrong_order_penalty         NUMERIC(5, 2) NOT NULL DEFAULT -1,
    ADD CONSTRAINT task_superfluous_columns_penalty_ck CHECK (superfluous_columns_penalty >= -1),
    ADD CONSTRAINT task_missing_rows_penalty_ck CHECK (missing_rows_penalty >= -1),
    ADD CONSTRAINT task_superfluous_rows_penalty_ck CHECK (superfluous_rows_penalty >= -1),
    ADD CONSTRAINT task_wrong_order_penalty_ck CHECK (wrong_order_penalty >= -1);
