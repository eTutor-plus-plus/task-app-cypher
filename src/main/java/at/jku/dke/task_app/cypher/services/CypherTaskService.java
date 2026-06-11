package at.jku.dke.task_app.cypher.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskInGroupService;
import at.jku.dke.task_app.cypher.data.entities.CypherEvaluationMode;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskGroupRepository;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskRepository;
import at.jku.dke.task_app.cypher.dto.AlternativeSolutionDto;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskDto;
import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import at.jku.dke.task_app.cypher.syntax.CypherQueryAnalyzer;
import at.jku.dke.task_app.cypher.syntax.CypherQueryStructure;
import at.jku.dke.task_app.cypher.syntax.CypherReturnColumnExtractor;
import at.jku.dke.task_app.cypher.syntax.CypherStatementValidator;
import at.jku.dke.task_app.cypher.syntax.CypherTaskDescriptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CypherTaskService extends BaseTaskInGroupService<CypherTask, CypherTaskGroup, ModifyCypherTaskDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CypherTaskService.class);
    private static final BigDecimal FULL_PERCENT = new BigDecimal("100");

    private final CypherStatementValidator statementValidator;
    private final CypherReturnColumnExtractor columnExtractor;
    private final CypherQueryAnalyzer queryAnalyzer;
    private final CypherTaskDescriptionBuilder descriptionBuilder;

    public CypherTaskService(CypherTaskRepository repository, CypherTaskGroupRepository taskGroupRepository,
                             CypherStatementValidator statementValidator, CypherReturnColumnExtractor columnExtractor,
                             CypherQueryAnalyzer queryAnalyzer, CypherTaskDescriptionBuilder descriptionBuilder) {
        super(repository, taskGroupRepository);
        this.statementValidator = statementValidator;
        this.columnExtractor = columnExtractor;
        this.queryAnalyzer = queryAnalyzer;
        this.descriptionBuilder = descriptionBuilder;
    }

    @Override
    protected CypherTask createTask(long id, ModifyTaskDto<ModifyCypherTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        ModifyCypherTaskDto data = modifyTaskDto.additionalData();
        CypherEvaluationMode mode = data.evaluationModeOrDefault();
        String primarySolution = resolvePrimarySolution(data, mode);
        this.statementValidator.validateReadQuery(primarySolution);
        if (mode == CypherEvaluationMode.MULTI_SOLUTION)
            validateAlternatives(data.alternativeSolutionsOrEmpty());

        CypherTask task = new CypherTask(
            primarySolution,
            data.superfluousColumnsPenalty(),
            data.missingRowsPenalty(),
            data.superfluousRowsPenalty(),
            data.wrongOrderPenalty(),
            normalize(data.expectedColumnNames()));
        task.setEvaluationMode(mode);
        if (mode == CypherEvaluationMode.MULTI_SOLUTION)
            task.replaceAlternativeSolutions(data.alternativeSolutionsOrEmpty());
        return task;
    }

    @Override
    protected void updateTask(CypherTask task, ModifyTaskDto<ModifyCypherTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        ModifyCypherTaskDto data = modifyTaskDto.additionalData();
        CypherEvaluationMode mode = data.evaluationModeOrDefault();
        String primarySolution = resolvePrimarySolution(data, mode);
        this.statementValidator.validateReadQuery(primarySolution);
        if (mode == CypherEvaluationMode.MULTI_SOLUTION)
            validateAlternatives(data.alternativeSolutionsOrEmpty());

        task.setEvaluationMode(mode);
        task.setSolution(primarySolution);
        task.setSuperfluousColumnsPenalty(data.superfluousColumnsPenalty());
        task.setMissingRowsPenalty(data.missingRowsPenalty());
        task.setSuperfluousRowsPenalty(data.superfluousRowsPenalty());
        task.setWrongOrderPenalty(data.wrongOrderPenalty());
        task.setExpectedColumnNames(normalize(data.expectedColumnNames()));
        if (mode == CypherEvaluationMode.MULTI_SOLUTION)
            task.replaceAlternativeSolutions(data.alternativeSolutionsOrEmpty());
        else
            task.replaceAlternativeSolutions(List.of());
    }

    private static String resolvePrimarySolution(ModifyCypherTaskDto data, CypherEvaluationMode mode) {
        if (mode == CypherEvaluationMode.MULTI_SOLUTION) {
            List<AlternativeSolutionDto> alternatives = data.alternativeSolutionsOrEmpty();
            if (alternatives.isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one alternative solution is required in MULTI_SOLUTION mode.");
            String solution = alternatives.stream()
                .max(Comparator.comparing(AlternativeSolutionDto::pointsPercent))
                .map(AlternativeSolutionDto::solution)
                .orElse(null);
            return solution == null ? "" : solution;
        }
        String solution = data.solution();
        if (solution == null || solution.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solution must not be blank in PENALTY mode.");
        return solution;
    }

    private void validateAlternatives(List<AlternativeSolutionDto> alternatives) {
        if (alternatives.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one alternative solution is required in MULTI_SOLUTION mode.");

        boolean hasFullPoints = alternatives.stream().anyMatch(alt -> alt.pointsPercent().compareTo(FULL_PERCENT) == 0);
        if (!hasFullPoints)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one alternative solution must award 100 % of maxPoints.");

        for (int i = 0; i < alternatives.size(); i++) {
            AlternativeSolutionDto alt = alternatives.get(i);
            try {
                this.statementValidator.validateReadQuery(alt.solution());
            } catch (CypherValidationException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Alternative solution at index " + i + " is invalid: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(CypherTask task, boolean create) {
        List<String> columns = resolveColumns(task);
        CypherQueryStructure structure = analyzeSolution(task);
        return new TaskModificationResponseDto(
            this.descriptionBuilder.build(Locale.GERMAN, structure, columns),
            this.descriptionBuilder.build(Locale.ENGLISH, structure, columns));
    }

    private CypherQueryStructure analyzeSolution(CypherTask task) {
        try {
            return this.queryAnalyzer.analyze(task.getSolution());
        } catch (RuntimeException ex) {
            LOG.debug("Could not analyze solution for description: {}", ex.getMessage());
            return null;
        }
    }

    private List<String> resolveColumns(CypherTask task) {
        String manual = task.getExpectedColumnNames();
        if (manual != null && !manual.isBlank()) {
            List<String> parsed = new ArrayList<>();
            for (String entry : manual.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty())
                    parsed.add(trimmed);
            }
            if (!parsed.isEmpty())
                return parsed;
        }

        try {
            return this.columnExtractor.extractColumnNames(task.getSolution());
        } catch (CypherValidationException ex) {
            LOG.debug("Could not auto-derive RETURN columns from solution: {}", ex.getMessage());
            return List.of();
        }
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
