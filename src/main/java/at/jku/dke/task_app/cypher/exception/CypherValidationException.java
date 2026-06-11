package at.jku.dke.task_app.cypher.exception;

public class CypherValidationException extends RuntimeException {
    public CypherValidationException(String message) {
        super(message);
    }
}
