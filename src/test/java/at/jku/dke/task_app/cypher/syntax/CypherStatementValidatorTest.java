package at.jku.dke.task_app.cypher.syntax;

import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherStatementValidatorTest {
    private final CypherStatementValidator validator = new CypherStatementValidator(new CypherStatementSplitter());

    @Test
    void validateReadQueryAcceptsReadOnlyCypher() {
        assertDoesNotThrow(() -> this.validator.validateReadQuery("""
            MATCH (p:Person)-[:WORKS_AT]->(d:Department)
            WHERE p.salary > 1000
            RETURN p.name AS name, d.name AS department
            ORDER BY name
            """));
    }

    @Test
    void validateReadQueryRejectsMutatingCypher() {
        assertThrows(CypherValidationException.class, () -> this.validator.validateReadQuery("MATCH (p) DELETE p"));
        assertThrows(CypherValidationException.class, () -> this.validator.validateReadQuery("MERGE (:Person {name: 'Ada'}) RETURN 1"));
        assertThrows(CypherValidationException.class, () -> this.validator.validateReadQuery("CALL db.labels()"));
    }

    @Test
    void validateReadQueryDoesNotRejectLabelsNamedLikeMutatingKeywords() {
        assertDoesNotThrow(() -> this.validator.validateReadQuery("MATCH (n:Delete)-[:Set]->(:Create) RETURN n"));
    }

    @Test
    void validateSetupRejectsDangerousClausesButAllowsGraphSetup() {
        assertDoesNotThrow(() -> this.validator.validateSetupScript("""
            CREATE (:User {name: 'Ada'});
            MATCH (u:User {name: 'Ada'}) SET u.active = true;
            """));
        assertThrows(CypherValidationException.class, () -> this.validator.validateSetupScript("LOAD CSV FROM 'file:///x.csv' AS row RETURN row"));
        assertThrows(CypherValidationException.class, () -> this.validator.validateSetupScript("CALL dbms.procedures()"));
        assertThrows(CypherValidationException.class, () -> this.validator.validateSetupScript("CREATE DATABASE other"));
    }

    @Test
    void containsOrderByIgnoresStringsAndComments() {
        assertTrue(this.validator.containsOrderBy("MATCH (n) RETURN n ORDER BY n.name"));
        assertTrue(!this.validator.containsOrderBy("RETURN 'ORDER BY' AS text // ORDER BY"));
    }
}
