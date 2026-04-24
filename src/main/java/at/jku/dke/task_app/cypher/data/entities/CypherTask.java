package at.jku.dke.task_app.cypher.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskInGroup;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Represents a cypher task.
 */
@Entity
@Table(name = "task")
public class CypherTask extends BaseTaskInGroup<CypherTaskGroup> {
    @NotNull
    @Column(name = "solution", nullable = false)
    private Integer solution;

    /**
     * Creates a new instance of class {@link CypherTask}.
     */
    public CypherTask() {
    }

    /**
     * Creates a new instance of class {@link CypherTask}.
     *
     * @param solution The solution.
     */
    public CypherTask(Integer solution) {
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link CypherTask}.
     *
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution.
     */
    public CypherTask(BigDecimal maxPoints, TaskStatus status, CypherTaskGroup taskGroup, Integer solution) {
        super(maxPoints, status, taskGroup);
        this.solution = solution;
    }

    /**
     * Creates a new instance of class {@link CypherTask}.
     *
     * @param id        The identifier.
     * @param maxPoints The maximum points.
     * @param status    The status.
     * @param taskGroup The task group.
     * @param solution  The solution.
     */
    public CypherTask(Long id, BigDecimal maxPoints, TaskStatus status, CypherTaskGroup taskGroup, Integer solution) {
        super(id, maxPoints, status, taskGroup);
        this.solution = solution;
    }

    /**
     * Gets the solution.
     *
     * @return The solution.
     */
    public Integer getSolution() {
        return solution;
    }

    /**
     * Sets the solution.
     *
     * @param solution The solution.
     */
    public void setSolution(Integer solution) {
        this.solution = solution;
    }
}
