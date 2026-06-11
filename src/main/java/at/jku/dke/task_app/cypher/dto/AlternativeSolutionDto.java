package at.jku.dke.task_app.cypher.dto;

import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

public record AlternativeSolutionDto(
    @NotBlank String solution,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal pointsPercent
) implements Serializable, CypherTask.AlternativeSolutionInput {
}
