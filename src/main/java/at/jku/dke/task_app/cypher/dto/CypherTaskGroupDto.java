package at.jku.dke.task_app.cypher.dto;

import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link CypherTaskGroup}
 *
 * @param minNumber The minimum number.
 * @param maxNumber The maximum number.
 */
public record CypherTaskGroupDto(@NotNull Integer minNumber, @NotNull Integer maxNumber) implements Serializable {
}
