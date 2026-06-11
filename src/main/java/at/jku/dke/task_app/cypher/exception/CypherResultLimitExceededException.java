package at.jku.dke.task_app.cypher.exception;

public class CypherResultLimitExceededException extends RuntimeException {
    public CypherResultLimitExceededException(int maxRows) {
        super("Query returned more than " + maxRows + " rows.");
    }
}
