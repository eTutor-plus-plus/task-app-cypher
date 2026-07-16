package at.jku.dke.task_app.cypher.dto;

import at.jku.dke.task_app.cypher.data.entities.CypherEvaluationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record ModifyCypherTaskDto(
    CypherEvaluationMode evaluationMode,
    String solution,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal superfluousColumnsPenalty,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal missingRowsPenalty,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal superfluousRowsPenalty,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal wrongOrderPenalty,
    @Valid List<AlternativeSolutionDto> alternativeSolutions
) implements Serializable {

    public CypherEvaluationMode evaluationModeOrDefault() {
        return evaluationMode == null ? CypherEvaluationMode.PENALTY : evaluationMode;
    }

    public List<AlternativeSolutionDto> alternativeSolutionsOrEmpty() {
        return alternativeSolutions == null ? List.of() : alternativeSolutions;
    }
}
