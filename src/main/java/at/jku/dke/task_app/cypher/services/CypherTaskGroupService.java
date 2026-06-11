package at.jku.dke.task_app.cypher.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskGroupRepository;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskGroupDto;
import at.jku.dke.task_app.cypher.evaluation.CypherAnalyzer;
import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import at.jku.dke.task_app.cypher.graph.CypherGraphPngExporter;
import at.jku.dke.task_app.cypher.graph.CypherTaskGroupDescriptionBuilder;
import at.jku.dke.task_app.cypher.graph.GraphSnapshot;
import at.jku.dke.task_app.cypher.graph.GraphSummary;
import at.jku.dke.task_app.cypher.syntax.CypherStatementValidator;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

@Service
public class CypherTaskGroupService extends BaseTaskGroupService<CypherTaskGroup, ModifyCypherTaskGroupDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CypherTaskGroupService.class);

    private final MessageSource messageSource;
    private final CypherStatementValidator statementValidator;
    private final CypherAnalyzer analyzer;
    private final CypherTaskGroupDescriptionBuilder descriptionBuilder;

    public CypherTaskGroupService(CypherTaskGroupRepository repository,
                                  MessageSource messageSource,
                                  CypherStatementValidator statementValidator,
                                  CypherAnalyzer analyzer,
                                  CypherTaskGroupDescriptionBuilder descriptionBuilder) {
        super(repository);
        this.messageSource = messageSource;
        this.statementValidator = statementValidator;
        this.analyzer = analyzer;
        this.descriptionBuilder = descriptionBuilder;
    }

    @Override
    protected CypherTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifyCypherTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");

        String setupStatements = modifyTaskGroupDto.additionalData().setupStatements();
        String secondarySetup = modifyTaskGroupDto.additionalData().secondarySetupStatements();
        this.statementValidator.validateSetupScript(setupStatements);
        this.statementValidator.validateSetupScript(secondarySetup);

        CypherTaskGroup taskGroup = new CypherTaskGroup(setupStatements, secondarySetup);
        this.refreshGraphImages(taskGroup, true);
        return taskGroup;
    }

    @Override
    protected void updateTaskGroup(CypherTaskGroup taskGroup, ModifyTaskGroupDto<ModifyCypherTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");

        String newSetup = modifyTaskGroupDto.additionalData().setupStatements();
        String newSecondarySetup = modifyTaskGroupDto.additionalData().secondarySetupStatements();
        this.statementValidator.validateSetupScript(newSetup);
        this.statementValidator.validateSetupScript(newSecondarySetup);

        boolean setupChanged = !Objects.equals(taskGroup.getSetupStatements(), newSetup);
        taskGroup.setSetupStatements(newSetup);
        taskGroup.setSecondarySetupStatements(newSecondarySetup);

        boolean rerender = setupChanged || taskGroup.getImageBase64De() == null || taskGroup.getImageBase64En() == null;
        this.refreshGraphImages(taskGroup, rerender);
    }

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(CypherTaskGroup taskGroup, boolean create) {
        GraphSummary summary = this.extractSummary(taskGroup.getSetupStatements());
        return new TaskGroupModificationResponseDto(
            this.descriptionBuilder.build(Locale.GERMAN, summary, taskGroup.getImageBase64De(), taskGroup.isImageTruncated()),
            this.descriptionBuilder.build(Locale.ENGLISH, summary, taskGroup.getImageBase64En(), taskGroup.isImageTruncated()));
    }

    private GraphSummary extractSummary(String setupStatements) {
        try {
            return GraphSummary.from(this.analyzer.extractGraph(setupStatements));
        } catch (CypherValidationException | Neo4jException ex) {
            LOG.warn("Could not extract graph summary for task group description: {}", ex.getMessage());
            return GraphSummary.from(null);
        }
    }

    private void refreshGraphImages(CypherTaskGroup taskGroup, boolean rerenderImages) {
        try {
            GraphSnapshot snapshot = this.analyzer.extractGraph(taskGroup.getSetupStatements());
            taskGroup.setImageTruncated(snapshot.truncated());

            if (!rerenderImages)
                return;

            if (snapshot.nodes().isEmpty()) {
                taskGroup.setImageBase64De(null);
                taskGroup.setImageBase64En(null);
                return;
            }

            long seed = taskGroup.getSetupStatements() == null ? 0L : taskGroup.getSetupStatements().hashCode();
            BufferedImage de = CypherGraphPngExporter.render(snapshot, "de", seed);
            BufferedImage en = CypherGraphPngExporter.render(snapshot, "en", seed);
            taskGroup.setImageBase64De(toBase64(de));
            taskGroup.setImageBase64En(toBase64(en));
        } catch (CypherValidationException | Neo4jException ex) {
            LOG.warn("Could not extract graph for task group: {}", ex.getMessage());
            taskGroup.setImageBase64De(null);
            taskGroup.setImageBase64En(null);
            taskGroup.setImageTruncated(false);
        }
    }

    private static String toBase64(BufferedImage img) {
        if (img == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("PNG encoding failed", ex);
        }
    }
}
