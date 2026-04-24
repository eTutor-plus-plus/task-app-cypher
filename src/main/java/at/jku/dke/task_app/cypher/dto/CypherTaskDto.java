package at.jku.dke.task_app.cypher.dto;

import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link CypherTask}
 *
 * @param solution The solution.
 */
public record CypherTaskDto(@NotNull Integer solution) implements Serializable {
}
