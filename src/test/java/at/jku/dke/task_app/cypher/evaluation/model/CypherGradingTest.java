package at.jku.dke.task_app.cypher.evaluation.model;

import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherGradingTest {

    private static final BigDecimal MAX = BigDecimal.TEN;

    @Test
    void correctSubmissionEarnsMaxPoints() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice"), row("Bob")));
        var submission = result(List.of("name"), List.of(row("Alice"), row("Bob")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, true));

        assertTrue(grading.isCorrect());
        assertEquals(0, MAX.compareTo(grading.getPoints()));
        assertTrue(grading.getDetails().isEmpty());
    }

    @Test
    void missingRowsDeductsConfiguredPercentage() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice"), row("Bob")));
        var submission = result(List.of("name"), List.of(row("Alice")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertFalse(grading.isCorrect());
        assertEquals(0, points("7.00").compareTo(grading.getPoints()));
        assertEquals(1, grading.getDetails().size());
        assertEquals(CypherEvaluationCriterion.MISSING_ROWS, grading.getDetails().get(0).criterion());
    }

    @Test
    void superfluousRowsDeductsConfiguredPercentage() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice")));
        var submission = result(List.of("name"), List.of(row("Alice"), row("Bob")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertEquals(0, points("6.00").compareTo(grading.getPoints()));
        assertEquals(CypherEvaluationCriterion.SUPERFLUOUS_ROWS, grading.getDetails().get(0).criterion());
    }

    @Test
    void wrongValuesDeductsBothMissingAndSuperfluous() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice")));
        var submission = result(List.of("name"), List.of(row("Eve")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertEquals(0, points("3.00").compareTo(grading.getPoints()));
        assertEquals(2, grading.getDetails().size());
    }

    @Test
    void wrongOrderDeductsConfiguredPercentage() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice"), row("Bob")));
        var submission = result(List.of("name"), List.of(row("Bob"), row("Alice")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, true));

        assertEquals(0, points("9.00").compareTo(grading.getPoints()));
        assertEquals(CypherEvaluationCriterion.CORRECT_ORDER, grading.getDetails().get(0).criterion());
    }

    @Test
    void superfluousRowsAndWrongOrderDeductBothPenalties() {
        var task = task(percent(20), percent(30), percent(50), percent(50));
        var solution = result(List.of("name"), List.of(row("Bob"), row("Alice")));
        var submission = result(List.of("name"), List.of(row("Alice"), row("Bob"), row("Carol")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, true));

        assertEquals(0, BigDecimal.ZERO.compareTo(grading.getPoints()));
        assertEquals(2, grading.getDetails().size());
        assertEquals(CypherEvaluationCriterion.SUPERFLUOUS_ROWS, grading.getDetails().get(0).criterion());
        assertEquals(CypherEvaluationCriterion.CORRECT_ORDER, grading.getDetails().get(1).criterion());
    }

    @Test
    void superfluousRowsWithPreservedOrderDoNotDeductOrderPenalty() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice"), row("Bob")));
        var submission = result(List.of("name"), List.of(row("Alice"), row("Bob"), row("Carol")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, true));

        assertEquals(0, points("6.00").compareTo(grading.getPoints()));
        assertEquals(1, grading.getDetails().size());
        assertEquals(CypherEvaluationCriterion.SUPERFLUOUS_ROWS, grading.getDetails().get(0).criterion());
    }

    @Test
    void columnsWrongDeductsOnlyColumnPenaltyAndSkipsRowChecks() {
        var task = task(percent(50), percent(30), percent(40), percent(10));
        var solution = result(List.of("name"), List.of(row("Alice")));
        var submission = result(List.of("nom"), List.of(row("Eve")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertEquals(0, points("5.00").compareTo(grading.getPoints()));
        assertEquals(1, grading.getDetails().size());
        assertEquals(CypherEvaluationCriterion.CORRECT_COLUMNS, grading.getDetails().get(0).criterion());
    }

    @Test
    void hundredPercentMeansFullDeduction() {
        var task = task(percent(100), percent(100), percent(100), percent(100));
        var solution = result(List.of("name"), List.of(row("Alice"), row("Bob")));
        var submission = result(List.of("name"), List.of(row("Alice")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertEquals(0, BigDecimal.ZERO.compareTo(grading.getPoints()));
    }

    @Test
    void totalCannotGoBelowZero() {
        var task = task(percent(80), percent(80), percent(80), percent(80));
        var solution = result(List.of("name"), List.of(row("Alice")));
        var submission = result(List.of("name"), List.of(row("Eve")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertEquals(0, BigDecimal.ZERO.compareTo(grading.getPoints()));
    }

    @Test
    void fractionalPercentageRoundsToTwoDecimals() {
        var task = task(percent(0), new BigDecimal("33.33"), percent(0), percent(0));
        var solution = result(List.of("name"), List.of(row("Alice"), row("Bob")));
        var submission = result(List.of("name"), List.of(row("Alice")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertEquals(0, points("6.67").compareTo(grading.getPoints()));
    }

    @Test
    void typoInColumnNameDeductsColumnPenalty() {
        var task = task(percent(20), percent(30), percent(40), percent(10));
        var solution = result(List.of("lastname"), List.of(row("Smith")));
        var submission = result(List.of("lasname"), List.of(row("Smith")));

        var grading = CypherGrading.of(task, new CypherComparison(solution, submission, false));

        assertFalse(grading.isCorrect());
        assertEquals(0, points("8.00").compareTo(grading.getPoints()));
        assertEquals(1, grading.getDetails().size());
        assertEquals(CypherEvaluationCriterion.CORRECT_COLUMNS, grading.getDetails().get(0).criterion());
    }

    @Test
    void syntaxErrorYieldsZeroAndSyntaxCriterion() {
        var task = task(percent(20), percent(30), percent(40), percent(10));

        var grading = CypherGrading.forSyntaxError(task);

        assertFalse(grading.isCorrect());
        assertEquals(0, BigDecimal.ZERO.compareTo(grading.getPoints()));
        assertEquals(1, grading.getDetails().size());
        assertEquals(CypherEvaluationCriterion.CORRECT_SYNTAX, grading.getDetails().get(0).criterion());
    }

    private static BigDecimal percent(int value) {
        return BigDecimal.valueOf(value);
    }

    private static BigDecimal points(String value) {
        return new BigDecimal(value);
    }

    private static CypherTask task(BigDecimal cols, BigDecimal missing, BigDecimal superfluous, BigDecimal order) {
        var task = new CypherTask("MATCH (n) RETURN n", cols, missing, superfluous, order, null);
        task.setMaxPoints(MAX);
        return task;
    }

    private static CypherQueryResult result(List<String> keys, List<List<CanonicalValue>> rows) {
        return new CypherQueryResult(keys, rows);
    }

    private static List<CanonicalValue> row(String value) {
        return List.of(new CanonicalValue(value, value));
    }
}
