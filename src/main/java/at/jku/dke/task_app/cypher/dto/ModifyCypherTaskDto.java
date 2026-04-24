package at.jku.dke.task_app.cypher.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying a cypher task.
 *
 * @param solution The solution.
 */
public record ModifyCypherTaskDto(@NotNull Integer solution) implements Serializable {
}
