package at.jku.dke.task_app.cypher.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskInGroup;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task")
public class CypherTask extends BaseTaskInGroup<CypherTaskGroup> {
    @NotNull
    @Column(name = "solution", nullable = false, columnDefinition = "TEXT")
    private String solution;

    @NotNull
    @Column(name = "superfluous_columns_penalty", precision = 5, scale = 2, nullable = false)
    private BigDecimal superfluousColumnsPenalty;

    @NotNull
    @Column(name = "missing_rows_penalty", precision = 5, scale = 2, nullable = false)
    private BigDecimal missingRowsPenalty;

    @NotNull
    @Column(name = "superfluous_rows_penalty", precision = 5, scale = 2, nullable = false)
    private BigDecimal superfluousRowsPenalty;

    @NotNull
    @Column(name = "wrong_order_penalty", precision = 5, scale = 2, nullable = false)
    private BigDecimal wrongOrderPenalty;

    @Column(name = "expected_column_names", columnDefinition = "TEXT")
    private String expectedColumnNames;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_mode", nullable = false, length = 32)
    private CypherEvaluationMode evaluationMode = CypherEvaluationMode.PENALTY;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id.ordinal ASC")
    private List<CypherTaskAlternativeSolution> alternativeSolutions = new ArrayList<>();

    public CypherTask() {
    }

    public CypherTask(String solution,
                      BigDecimal superfluousColumnsPenalty,
                      BigDecimal missingRowsPenalty,
                      BigDecimal superfluousRowsPenalty,
                      BigDecimal wrongOrderPenalty,
                      String expectedColumnNames) {
        this.solution = solution;
        this.superfluousColumnsPenalty = superfluousColumnsPenalty;
        this.missingRowsPenalty = missingRowsPenalty;
        this.superfluousRowsPenalty = superfluousRowsPenalty;
        this.wrongOrderPenalty = wrongOrderPenalty;
        this.expectedColumnNames = expectedColumnNames;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public BigDecimal getSuperfluousColumnsPenalty() {
        return superfluousColumnsPenalty;
    }

    public void setSuperfluousColumnsPenalty(BigDecimal superfluousColumnsPenalty) {
        this.superfluousColumnsPenalty = superfluousColumnsPenalty;
    }

    public BigDecimal getMissingRowsPenalty() {
        return missingRowsPenalty;
    }

    public void setMissingRowsPenalty(BigDecimal missingRowsPenalty) {
        this.missingRowsPenalty = missingRowsPenalty;
    }

    public BigDecimal getSuperfluousRowsPenalty() {
        return superfluousRowsPenalty;
    }

    public void setSuperfluousRowsPenalty(BigDecimal superfluousRowsPenalty) {
        this.superfluousRowsPenalty = superfluousRowsPenalty;
    }

    public BigDecimal getWrongOrderPenalty() {
        return wrongOrderPenalty;
    }

    public void setWrongOrderPenalty(BigDecimal wrongOrderPenalty) {
        this.wrongOrderPenalty = wrongOrderPenalty;
    }

    public String getExpectedColumnNames() {
        return expectedColumnNames;
    }

    public void setExpectedColumnNames(String expectedColumnNames) {
        this.expectedColumnNames = expectedColumnNames;
    }

    public CypherEvaluationMode getEvaluationMode() {
        return evaluationMode;
    }

    public void setEvaluationMode(CypherEvaluationMode evaluationMode) {
        this.evaluationMode = evaluationMode;
    }

    public List<CypherTaskAlternativeSolution> getAlternativeSolutions() {
        return alternativeSolutions;
    }

    public void setAlternativeSolutions(List<CypherTaskAlternativeSolution> alternativeSolutions) {
        this.alternativeSolutions.clear();
        if (alternativeSolutions != null)
            this.alternativeSolutions.addAll(alternativeSolutions);
    }

    public void replaceAlternativeSolutions(List<? extends AlternativeSolutionInput> inputs) {
        this.alternativeSolutions.clear();
        if (inputs == null)
            return;
        int ordinal = 0;
        for (AlternativeSolutionInput in : inputs) {
            this.alternativeSolutions.add(new CypherTaskAlternativeSolution(this, ordinal++, in.solution(), in.pointsPercent()));
        }
    }

    public interface AlternativeSolutionInput {
        String solution();

        BigDecimal pointsPercent();
    }
}
