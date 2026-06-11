package at.jku.dke.task_app.cypher.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "task_alternative_solution")
public class CypherTaskAlternativeSolution {

    @EmbeddedId
    private Id id;

    @MapsId("taskId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private CypherTask task;

    @NotNull
    @Column(name = "solution", nullable = false, columnDefinition = "TEXT")
    private String solution;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    @Column(name = "points_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal pointsPercent;

    public CypherTaskAlternativeSolution() {
    }

    public CypherTaskAlternativeSolution(CypherTask task, int ordinal, String solution, BigDecimal pointsPercent) {
        this.task = task;
        this.id = new Id(task == null ? null : task.getId(), ordinal);
        this.solution = solution;
        this.pointsPercent = pointsPercent;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public CypherTask getTask() {
        return task;
    }

    public void setTask(CypherTask task) {
        this.task = task;
        if (this.id != null)
            this.id.setTaskId(task == null ? null : task.getId());
    }

    public int getOrdinal() {
        return id == null ? 0 : id.getOrdinal();
    }

    public void setOrdinal(int ordinal) {
        if (this.id == null)
            this.id = new Id(null, ordinal);
        else
            this.id.setOrdinal(ordinal);
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public BigDecimal getPointsPercent() {
        return pointsPercent;
    }

    public void setPointsPercent(BigDecimal pointsPercent) {
        this.pointsPercent = pointsPercent;
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "task_id")
        private Long taskId;

        @Column(name = "ordinal", nullable = false)
        private int ordinal;

        public Id() {
        }

        public Id(Long taskId, int ordinal) {
            this.taskId = taskId;
            this.ordinal = ordinal;
        }

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id other)) return false;
            return ordinal == other.ordinal && Objects.equals(taskId, other.taskId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, ordinal);
        }
    }
}
