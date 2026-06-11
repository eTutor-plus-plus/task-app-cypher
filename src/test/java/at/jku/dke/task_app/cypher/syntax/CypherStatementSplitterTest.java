package at.jku.dke.task_app.cypher.syntax;

import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherStatementSplitterTest {
    private final CypherStatementSplitter splitter = new CypherStatementSplitter();

    @Test
    void splitIgnoresSemicolonsInStringsBackticksAndComments() {
        String script = """
            CREATE (:Text {value: 'one;two'});
            CREATE (:`Semi;Label` {name: "three;four"}); // ignored; semicolon
            /* ignored; block */
            MATCH (n) RETURN n;
            """;

        var result = this.splitter.split(script);

        assertEquals(3, result.size());
        assertEquals("CREATE (:Text {value: 'one;two'})", result.get(0));
        assertEquals("CREATE (:`Semi;Label` {name: \"three;four\"})", result.get(1));
        assertTrue(result.get(2).contains("MATCH (n) RETURN n"));
    }

    @Test
    void splitRejectsUnterminatedString() {
        assertThrows(CypherValidationException.class, () -> this.splitter.split("RETURN 'unterminated"));
    }

    @Test
    void splitIgnoresTrailingCommentOnlyStatement() {
        var result = this.splitter.split("MATCH (n) RETURN n; // trailing comment");

        assertEquals(1, result.size());
        assertEquals("MATCH (n) RETURN n", result.getFirst());
    }
}
