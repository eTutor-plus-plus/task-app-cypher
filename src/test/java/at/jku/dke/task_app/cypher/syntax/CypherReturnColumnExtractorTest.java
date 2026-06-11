package at.jku.dke.task_app.cypher.syntax;

import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CypherReturnColumnExtractorTest {

    private final CypherReturnColumnExtractor extractor = new CypherReturnColumnExtractor();

    @Test
    void returnsExpressionWhenNoAlias() {
        assertEquals(List.of("p.name"), extractor.extractColumnNames("MATCH (p:Person) RETURN p.name"));
    }

    @Test
    void returnsAliasAfterAs() {
        assertEquals(List.of("name", "age"),
            extractor.extractColumnNames("MATCH (p:Person) RETURN p.name AS name, p.age AS age"));
    }

    @Test
    void mixedAliasedAndPlain() {
        assertEquals(List.of("p.name", "total"),
            extractor.extractColumnNames("MATCH (p:Person) RETURN p.name, count(*) AS total"));
    }

    @Test
    void stripsLeadingDistinct() {
        assertEquals(List.of("p.name"),
            extractor.extractColumnNames("MATCH (p:Person) RETURN DISTINCT p.name"));
    }

    @Test
    void stopsAtOrderBy() {
        assertEquals(List.of("p.name"),
            extractor.extractColumnNames("MATCH (p:Person) RETURN p.name ORDER BY p.name"));
    }

    @Test
    void stopsAtLimit() {
        assertEquals(List.of("p.name"),
            extractor.extractColumnNames("MATCH (p:Person) RETURN p.name LIMIT 10"));
    }

    @Test
    void stopsAtSkip() {
        assertEquals(List.of("p.name"),
            extractor.extractColumnNames("MATCH (p:Person) RETURN p.name SKIP 5"));
    }

    @Test
    void stopsAtUnionUsingFirstReturn() {
        assertEquals(List.of("name"),
            extractor.extractColumnNames("MATCH (a:Person) RETURN a.name AS name UNION MATCH (b:Movie) RETURN b.title AS name"));
    }

    @Test
    void ignoresReturnInsideStringLiteral() {
        assertEquals(List.of("n.label"),
            extractor.extractColumnNames("MATCH (n) WHERE n.note = 'no RETURN here' RETURN n.label"));
    }

    @Test
    void ignoresReturnInsideBacktickIdentifier() {
        assertEquals(List.of("x"),
            extractor.extractColumnNames("MATCH (n {`some RETURN ident`: 1}) RETURN n.x AS x"));
    }

    @Test
    void ignoresReturnInsideLineComment() {
        assertEquals(List.of("x"),
            extractor.extractColumnNames("// nothing to RETURN\nMATCH (n) RETURN n.x AS x"));
    }

    @Test
    void ignoresReturnInsideBlockComment() {
        assertEquals(List.of("x"),
            extractor.extractColumnNames("MATCH (n) /* RETURN inside */ RETURN n.x AS x"));
    }

    @Test
    void honorsTopLevelAsAcrossNestedExpression() {
        assertEquals(List.of("category"),
            extractor.extractColumnNames(
                "MATCH (p:Person) RETURN CASE WHEN p.age > 18 THEN 'adult' ELSE 'minor' END AS category"));
    }

    @Test
    void unquotesBacktickedAlias() {
        assertEquals(List.of("with space"),
            extractor.extractColumnNames("MATCH (n) RETURN n.x AS `with space`"));
    }

    @Test
    void caseInsensitiveKeywords() {
        assertEquals(List.of("name"),
            extractor.extractColumnNames("match (p:Person) return p.name as name"));
    }

    @Test
    void throwsWhenNoReturn() {
        assertThrows(CypherValidationException.class,
            () -> extractor.extractColumnNames("MATCH (n) DELETE n"));
    }

    @Test
    void throwsWhenEmptyQuery() {
        assertThrows(CypherValidationException.class,
            () -> extractor.extractColumnNames("   "));
    }

    @Test
    void splitsAtTopLevelCommaButNotInsideParensOrBraces() {
        assertEquals(List.of("listOfTwo", "fromMap"),
            extractor.extractColumnNames(
                "RETURN [1, 2] AS listOfTwo, {a: 1, b: 2}.a AS fromMap"));
    }
}
