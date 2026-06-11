package at.jku.dke.task_app.cypher.data.entities;

import at.jku.dke.etutor.task_app.data.entities.BaseTaskGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "task_group")
public class CypherTaskGroup extends BaseTaskGroup {
    @NotNull
    @Column(name = "setup_statements", nullable = false, columnDefinition = "TEXT")
    private String setupStatements;

    @NotNull
    @Column(name = "secondary_setup_statements", nullable = false, columnDefinition = "TEXT")
    private String secondarySetupStatements;

    @Column(name = "image_base64_de", columnDefinition = "TEXT")
    private String imageBase64De;

    @Column(name = "image_base64_en", columnDefinition = "TEXT")
    private String imageBase64En;

    @Column(name = "image_truncated", nullable = false)
    private boolean imageTruncated;

    public CypherTaskGroup() {
    }

    public CypherTaskGroup(String setupStatements, String secondarySetupStatements) {
        this.setupStatements = setupStatements;
        this.secondarySetupStatements = secondarySetupStatements;
    }

    public String getSetupStatements() {
        return setupStatements;
    }

    public void setSetupStatements(String setupStatements) {
        this.setupStatements = setupStatements;
    }

    public String getSecondarySetupStatements() {
        return secondarySetupStatements;
    }

    public void setSecondarySetupStatements(String secondarySetupStatements) {
        this.secondarySetupStatements = secondarySetupStatements;
    }

    public String getImageBase64De() {
        return imageBase64De;
    }

    public void setImageBase64De(String imageBase64De) {
        this.imageBase64De = imageBase64De;
    }

    public String getImageBase64En() {
        return imageBase64En;
    }

    public void setImageBase64En(String imageBase64En) {
        this.imageBase64En = imageBase64En;
    }

    public boolean isImageTruncated() {
        return imageTruncated;
    }

    public void setImageTruncated(boolean imageTruncated) {
        this.imageTruncated = imageTruncated;
    }
}
