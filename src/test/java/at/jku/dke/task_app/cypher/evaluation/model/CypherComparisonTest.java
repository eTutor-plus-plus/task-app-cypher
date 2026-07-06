package at.jku.dke.task_app.cypher.evaluation.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherComparisonTest {

    @Test
    void exactlyMatchingKeysProduceNoNearMatches() {
        var comparison = new CypherComparison(
            result(List.of("name", "age"), List.of(row("Alice", "30"))),
            result(List.of("name", "age"), List.of(row("Alice", "30"))),
            false);

        assertTrue(comparison.isCorrect());
        assertTrue(comparison.nearMatches().isEmpty());
    }

    @Test
    void smallTypoIsReportedAsHintButKeysAreStillIncorrect() {
        var comparison = new CypherComparison(
            result(List.of("lastname"), List.of(row("Smith"))),
            result(List.of("lasname"), List.of(row("Smith"))),
            false);

        assertFalse(comparison.keysCorrect());
        assertFalse(comparison.rowsCorrect());
        assertEquals(1, comparison.nearMatches().size());
        assertEquals("lastname", comparison.nearMatches().get("lasname"));
        assertTrue(comparison.superfluousKeys().isEmpty());
        assertTrue(comparison.missingKeys().isEmpty());
    }

    @Test
    void typoStaysSuperfluousIfTooFarFromAnySolutionKey() {
        var comparison = new CypherComparison(
            result(List.of("lastname"), List.of(row("Smith"))),
            result(List.of("totally_different"), List.of(row("Smith"))),
            false);

        assertFalse(comparison.keysCorrect());
        assertTrue(comparison.nearMatches().isEmpty());
        assertEquals(List.of("totally_different"), comparison.superfluousKeys());
        assertEquals(List.of("lastname"), comparison.missingKeys());
    }

    @Test
    void shortNamesAllowAtMostOneEdit() {
        var comparison = new CypherComparison(
            result(List.of("age"), List.of(row("30"))),
            result(List.of("xy"), List.of(row("30"))),
            false);

        assertFalse(comparison.keysCorrect());
        assertTrue(comparison.nearMatches().isEmpty());
    }

    @Test
    void onlyOneSubmissionKeyMatchesEachSolutionKey() {
        var comparison = new CypherComparison(
            result(List.of("lastname"), List.of(row("Smith"))),
            result(List.of("lasname", "lastnam"), List.of(row("Smith", "Smith"))),
            false);

        assertEquals(1, comparison.nearMatches().size());
        assertEquals("lastname", comparison.nearMatches().get("lasname"));
        assertEquals(List.of("lastnam"), comparison.superfluousKeys());
        assertFalse(comparison.keysCorrect());
    }

    @Test
    void typoMakesSubmissionIncorrectEvenWhenRowsWouldOtherwiseMatch() {
        var comparison = new CypherComparison(
            result(List.of("lastname", "age"), List.of(row("Smith", "30"))),
            result(List.of("lasname", "age"), List.of(row("Smith", "30"))),
            false);

        assertFalse(comparison.isCorrect());
        assertFalse(comparison.keysCorrect());
        assertEquals(1, comparison.nearMatches().size());
        assertEquals("lastname", comparison.nearMatches().get("lasname"));
    }

    @Test
    void superfluousRowWithPreservedOrderKeepsOrderCorrect() {
        var comparison = new CypherComparison(
            result(List.of("name"), List.of(row("Alice"), row("Bob"))),
            result(List.of("name"), List.of(row("Alice"), row("Bob"), row("Carol"))),
            true);

        assertFalse(comparison.rowsCorrect());
        assertTrue(comparison.orderCorrect());
    }

    @Test
    void superfluousRowWithSwappedCommonRowsMakesOrderIncorrect() {
        var comparison = new CypherComparison(
            result(List.of("name"), List.of(row("Bob"), row("Alice"))),
            result(List.of("name"), List.of(row("Alice"), row("Bob"), row("Carol"))),
            true);

        assertFalse(comparison.rowsCorrect());
        assertFalse(comparison.orderCorrect());
    }

    @Test
    void missingRowDoesNotAffectOrderOfRemainingRows() {
        var comparison = new CypherComparison(
            result(List.of("name"), List.of(row("Alice"), row("Bob"), row("Carol"))),
            result(List.of("name"), List.of(row("Alice"), row("Carol"))),
            true);

        assertFalse(comparison.rowsCorrect());
        assertTrue(comparison.orderCorrect());
    }

    @Test
    void solutionKeyAlreadyMatchedExactlyIsNotReusedForTypo() {
        var comparison = new CypherComparison(
            result(List.of("name"), List.of(row("Alice"))),
            result(List.of("name", "namee"), List.of(row("Alice", "Alice"))),
            false);

        assertTrue(comparison.nearMatches().isEmpty());
        assertEquals(List.of("namee"), comparison.superfluousKeys());
    }

    private static CypherQueryResult result(List<String> keys, List<List<CanonicalValue>> rows) {
        return new CypherQueryResult(keys, rows);
    }

    private static List<CanonicalValue> row(String... values) {
        return java.util.Arrays.stream(values).map(v -> new CanonicalValue(v, v)).toList();
    }
}
