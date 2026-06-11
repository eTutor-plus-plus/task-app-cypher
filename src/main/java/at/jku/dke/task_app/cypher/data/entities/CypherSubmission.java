package at.jku.dke.task_app.cypher.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseSubmission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "submission")
public class CypherSubmission extends BaseSubmission<CypherTask> {
    @NotNull
    @Column(name = "submission", nullable = false, columnDefinition = "TEXT")
    private String submission;

    public CypherSubmission() {
    }

    public CypherSubmission(String submission) {
        this.submission = submission;
    }

    public String getSubmission() {
        return submission;
    }

    public void setSubmission(String submission) {
        this.submission = submission;
    }
}
