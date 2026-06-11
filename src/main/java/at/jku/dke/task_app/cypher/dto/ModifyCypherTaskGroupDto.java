package at.jku.dke.task_app.cypher.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record ModifyCypherTaskGroupDto(@NotBlank String setupStatements,
                                       @NotBlank String secondarySetupStatements) implements Serializable {
}
