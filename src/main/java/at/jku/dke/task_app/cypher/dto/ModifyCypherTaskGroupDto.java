package at.jku.dke.task_app.cypher.dto;

import at.jku.dke.task_app.cypher.validation.ValidTaskGroupNumber;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * This class represents a data transfer object for modifying a cypher task group.
 *
 * @param minNumber The minimum number.
 * @param maxNumber The maximum number.
 */
@ValidTaskGroupNumber
public record ModifyCypherTaskGroupDto(@NotNull Integer minNumber, @NotNull Integer maxNumber) implements Serializable {
}
