package at.jku.dke.task_app.cypher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record CypherSubmissionDto(@NotNull @NotBlank String input) {
}
