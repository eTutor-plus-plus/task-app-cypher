-- Migrate penalty columns from absolute points to percentages of max_points (0..100).
-- Historical "-1" sentinel ("full deduction") becomes 100. Existing absolute deductions are
-- rescaled to the equivalent percentage and capped at 100.
UPDATE task SET
    superfluous_columns_penalty = CASE
        WHEN superfluous_columns_penalty < 0 THEN 100
        WHEN max_points = 0              THEN 0
        ELSE LEAST(100, ROUND(superfluous_columns_penalty / max_points * 100, 2))
    END,
    missing_rows_penalty = CASE
        WHEN missing_rows_penalty < 0 THEN 100
        WHEN max_points = 0           THEN 0
        ELSE LEAST(100, ROUND(missing_rows_penalty / max_points * 100, 2))
    END,
    superfluous_rows_penalty = CASE
        WHEN superfluous_rows_penalty < 0 THEN 100
        WHEN max_points = 0               THEN 0
        ELSE LEAST(100, ROUND(superfluous_rows_penalty / max_points * 100, 2))
    END,
    wrong_order_penalty = CASE
        WHEN wrong_order_penalty < 0 THEN 100
        WHEN max_points = 0          THEN 0
        ELSE LEAST(100, ROUND(wrong_order_penalty / max_points * 100, 2))
    END;

ALTER TABLE task
    DROP CONSTRAINT task_superfluous_columns_penalty_ck,
    DROP CONSTRAINT task_missing_rows_penalty_ck,
    DROP CONSTRAINT task_superfluous_rows_penalty_ck,
    DROP CONSTRAINT task_wrong_order_penalty_ck,
    ALTER COLUMN superfluous_columns_penalty SET DEFAULT 100,
    ALTER COLUMN missing_rows_penalty        SET DEFAULT 100,
    ALTER COLUMN superfluous_rows_penalty    SET DEFAULT 100,
    ALTER COLUMN wrong_order_penalty         SET DEFAULT 100,
    ADD CONSTRAINT task_superfluous_columns_penalty_ck CHECK (superfluous_columns_penalty BETWEEN 0 AND 100),
    ADD CONSTRAINT task_missing_rows_penalty_ck        CHECK (missing_rows_penalty BETWEEN 0 AND 100),
    ADD CONSTRAINT task_superfluous_rows_penalty_ck    CHECK (superfluous_rows_penalty BETWEEN 0 AND 100),
    ADD CONSTRAINT task_wrong_order_penalty_ck         CHECK (wrong_order_penalty BETWEEN 0 AND 100);
