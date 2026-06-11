ALTER TABLE task
    ADD COLUMN evaluation_mode VARCHAR(32) NOT NULL DEFAULT 'PENALTY',
    ADD CONSTRAINT task_evaluation_mode_ck CHECK (evaluation_mode IN ('PENALTY', 'MULTI_SOLUTION'));

CREATE TABLE task_alternative_solution
(
    task_id        BIGINT       NOT NULL,
    ordinal        INT          NOT NULL,
    solution       TEXT         NOT NULL,
    points_percent NUMERIC(5, 2) NOT NULL,
    CONSTRAINT task_alternative_solution_pk PRIMARY KEY (task_id, ordinal),
    CONSTRAINT task_alternative_solution_task_fk FOREIGN KEY (task_id) REFERENCES task (id)
        ON DELETE CASCADE,
    CONSTRAINT task_alternative_solution_points_ck CHECK (points_percent BETWEEN 0 AND 100),
    CONSTRAINT task_alternative_solution_ordinal_ck CHECK (ordinal >= 0)
);
