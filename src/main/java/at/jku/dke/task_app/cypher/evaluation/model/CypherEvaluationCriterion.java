package at.jku.dke.task_app.cypher.evaluation.model;

public enum CypherEvaluationCriterion {
    CORRECT_SYNTAX,
    CORRECT_COLUMNS,
    MISSING_ROWS,
    SUPERFLUOUS_ROWS,
    CORRECT_ORDER
}
