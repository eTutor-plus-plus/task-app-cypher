package at.jku.dke.task_app.cypher.evaluation.model;

import at.jku.dke.task_app.cypher.data.entities.CypherTask;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CypherGrading {

    public record Entry(CypherEvaluationCriterion criterion, BigDecimal minusPoints) {
    }

    private final BigDecimal maxPoints;
    private final BigDecimal points;
    private final boolean correct;
    private final List<Entry> details;

    private CypherGrading(BigDecimal maxPoints, BigDecimal points, boolean correct, List<Entry> details) {
        this.maxPoints = maxPoints;
        this.points = points.setScale(2, RoundingMode.HALF_UP);
        this.correct = correct;
        this.details = details;
    }

    public static CypherGrading of(CypherTask task, CypherComparison comparison) {
        if (comparison.isCorrect())
            return new CypherGrading(task.getMaxPoints(), task.getMaxPoints(), true, List.of());

        List<Entry> entries = new ArrayList<>();
        if (!comparison.keysCorrect()) {
            entries.add(new Entry(CypherEvaluationCriterion.CORRECT_COLUMNS,
                resolvePenalty(task.getSuperfluousColumnsPenalty(), task.getMaxPoints())));
        } else {
            if (!comparison.missingRows().isEmpty())
                entries.add(new Entry(CypherEvaluationCriterion.MISSING_ROWS,
                    resolvePenalty(task.getMissingRowsPenalty(), task.getMaxPoints())));
            if (!comparison.superfluousRows().isEmpty())
                entries.add(new Entry(CypherEvaluationCriterion.SUPERFLUOUS_ROWS,
                    resolvePenalty(task.getSuperfluousRowsPenalty(), task.getMaxPoints())));
            if (comparison.orderRelevant() && !comparison.orderCorrect())
                entries.add(new Entry(CypherEvaluationCriterion.CORRECT_ORDER,
                    resolvePenalty(task.getWrongOrderPenalty(), task.getMaxPoints())));
        }

        BigDecimal deduction = entries.stream().map(Entry::minusPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = task.getMaxPoints().subtract(deduction).max(BigDecimal.ZERO);
        return new CypherGrading(task.getMaxPoints(), total, false, Collections.unmodifiableList(entries));
    }

    public static CypherGrading forSyntaxError(CypherTask task) {
        return new CypherGrading(
            task.getMaxPoints(),
            BigDecimal.ZERO,
            false,
            List.of(new Entry(CypherEvaluationCriterion.CORRECT_SYNTAX, task.getMaxPoints())));
    }

    public BigDecimal getPoints() {
        return points;
    }

    public boolean isCorrect() {
        return correct;
    }

    public List<Entry> getDetails() {
        return details;
    }

    private static BigDecimal resolvePenalty(BigDecimal percent, BigDecimal maxPoints) {
        return percent.movePointLeft(2).multiply(maxPoints).setScale(2, RoundingMode.HALF_UP);
    }
}
