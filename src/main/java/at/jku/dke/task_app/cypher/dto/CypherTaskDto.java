package at.jku.dke.task_app.cypher.dto;

import at.jku.dke.task_app.cypher.data.entities.CypherEvaluationMode;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record CypherTaskDto(
    @NotNull CypherEvaluationMode evaluationMode,
    String solution,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal superfluousColumnsPenalty,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal missingRowsPenalty,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal superfluousRowsPenalty,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal wrongOrderPenalty,
    String expectedColumnNames,
    @Valid List<AlternativeSolutionDto> alternativeSolutions
) implements Serializable {
}
