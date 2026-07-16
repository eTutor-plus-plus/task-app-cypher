package at.jku.dke.task_app.cypher.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.cypher.data.entities.CypherEvaluationMode;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskAlternativeSolution;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskGroupRepository;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskRepository;
import at.jku.dke.task_app.cypher.dto.AlternativeSolutionDto;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskDto;
import at.jku.dke.task_app.cypher.syntax.CypherQueryAnalyzer;
import at.jku.dke.task_app.cypher.syntax.CypherReturnColumnExtractor;
import at.jku.dke.task_app.cypher.syntax.CypherStatementSplitter;
import at.jku.dke.task_app.cypher.syntax.CypherStatementValidator;
import at.jku.dke.task_app.cypher.syntax.CypherTaskDescriptionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CypherTaskServiceTest {

    private CypherTaskService service;

    @BeforeEach
    void setUp() {
        var splitter = new CypherStatementSplitter();
        var validator = new CypherStatementValidator(splitter);
        var columnExtractor = new CypherReturnColumnExtractor();
        var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");
        service = new CypherTaskService(
            Mockito.mock(CypherTaskRepository.class),
            Mockito.mock(CypherTaskGroupRepository.class),
            validator,
            columnExtractor,
            new CypherQueryAnalyzer(),
            new CypherTaskDescriptionBuilder(messageSource));
    }

    @Test
    void createTaskPenaltyMode() {
        ModifyCypherTaskDto data = new ModifyCypherTaskDto(
            CypherEvaluationMode.PENALTY,
            "MATCH (p:Person) RETURN p.name AS name",
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
            null);

        CypherTask task = invokeCreate(123L, modifyDto(data));

        assertEquals(CypherEvaluationMode.PENALTY, task.getEvaluationMode());
        assertEquals("MATCH (p:Person) RETURN p.name AS name", task.getSolution());
        assertEquals(0, task.getAlternativeSolutions().size());
    }

    @Test
    void createTaskMultiSolutionPersistsAlternatives() {
        ModifyCypherTaskDto data = multiSolutionData(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name, p.age AS age", new BigDecimal("100")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("60")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name LIMIT 1", new BigDecimal("30"))
        ));

        CypherTask task = invokeCreate(123L, modifyDto(data));

        assertEquals(CypherEvaluationMode.MULTI_SOLUTION, task.getEvaluationMode());
        assertEquals(3, task.getAlternativeSolutions().size());
        assertEquals("MATCH (p:Person) RETURN p.name AS name, p.age AS age", task.getSolution());
        List<CypherTaskAlternativeSolution> alternatives = task.getAlternativeSolutions();
        assertEquals(0, alternatives.get(0).getOrdinal());
        assertEquals(0, alternatives.get(0).getPointsPercent().compareTo(new BigDecimal("100")));
        assertEquals(1, alternatives.get(1).getOrdinal());
        assertEquals(0, alternatives.get(1).getPointsPercent().compareTo(new BigDecimal("60")));
        assertEquals(2, alternatives.get(2).getOrdinal());
    }

    @Test
    void createTaskMultiSolutionRejectsEmptyList() {
        ModifyCypherTaskDto data = multiSolutionData(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> invokeCreate(1L, modifyDto(data)));
        assertNotNull(ex.getReason());
    }

    @Test
    void createTaskMultiSolutionRejectsWhenNoHundredPercent() {
        ModifyCypherTaskDto data = multiSolutionData(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("90"))
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> invokeCreate(1L, modifyDto(data)));
        assertEquals(true, ex.getReason() != null && ex.getReason().contains("100"));
    }

    @Test
    void createTaskMultiSolutionAcceptsOutOfOrderAndPicksHundredAsPrimary() {
        ModifyCypherTaskDto data = multiSolutionData(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.age AS age", new BigDecimal("40")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name, p.age AS age", new BigDecimal("100")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("60"))
        ));

        CypherTask task = invokeCreate(1L, modifyDto(data));

        assertEquals(3, task.getAlternativeSolutions().size());
        assertEquals(0, task.getAlternativeSolutions().get(0).getPointsPercent().compareTo(new BigDecimal("40")));
        assertEquals("MATCH (p:Person) RETURN p.name AS name, p.age AS age", task.getSolution());
    }

    @Test
    void createTaskMultiSolutionAcceptsDuplicateHundredPercent() {
        ModifyCypherTaskDto data = multiSolutionData(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("100")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.age AS age", new BigDecimal("100"))
        ));

        CypherTask task = invokeCreate(1L, modifyDto(data));
        assertEquals(2, task.getAlternativeSolutions().size());
    }

    @Test
    void createTaskMultiSolutionRejectsInvalidAlternative() {
        ModifyCypherTaskDto data = multiSolutionData(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("100")),
            new AlternativeSolutionDto("CREATE (:X {a: 1}) RETURN 1", new BigDecimal("50")) // mutation forbidden
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> invokeCreate(1L, modifyDto(data)));
        assertEquals(true, ex.getReason() != null && ex.getReason().contains("index 1"));
    }

    @Test
    void updateTaskFromPenaltyToMultiClearsThenReplacesAlternatives() {
        CypherTask existing = new CypherTask(
            "MATCH (p:Person) RETURN p.name AS name",
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"));
        existing.setTaskGroup(new CypherTaskGroup("CREATE (:X);", "CREATE (:Y);"));
        existing.setMaxPoints(BigDecimal.TEN);
        existing.setStatus(TaskStatus.APPROVED);

        ModifyCypherTaskDto data = multiSolutionData(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name, p.age AS age", new BigDecimal("100")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("50"))
        ));

        invokeUpdate(existing, modifyDto(data));

        assertEquals(CypherEvaluationMode.MULTI_SOLUTION, existing.getEvaluationMode());
        assertEquals(2, existing.getAlternativeSolutions().size());
        assertEquals("MATCH (p:Person) RETURN p.name AS name, p.age AS age", existing.getSolution());
    }

    @Test
    void updateTaskFromMultiToPenaltyDropsAlternatives() {
        CypherTask existing = new CypherTask(
            "old",
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"));
        existing.setTaskGroup(new CypherTaskGroup("CREATE (:X);", "CREATE (:Y);"));
        existing.setMaxPoints(BigDecimal.TEN);
        existing.setStatus(TaskStatus.APPROVED);
        existing.setEvaluationMode(CypherEvaluationMode.MULTI_SOLUTION);
        existing.replaceAlternativeSolutions(List.of(
            new AlternativeSolutionDto("MATCH (p) RETURN p", new BigDecimal("100"))
        ));
        assertSame(1, existing.getAlternativeSolutions().size());

        ModifyCypherTaskDto data = new ModifyCypherTaskDto(
            CypherEvaluationMode.PENALTY,
            "MATCH (p:Person) RETURN p.name AS name",
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
            null);

        invokeUpdate(existing, modifyDto(data));

        assertEquals(CypherEvaluationMode.PENALTY, existing.getEvaluationMode());
        assertEquals(0, existing.getAlternativeSolutions().size());
        assertEquals("MATCH (p:Person) RETURN p.name AS name", existing.getSolution());
    }

    @Test
    void createTaskNullEvaluationModeDefaultsToPenalty() {
        ModifyCypherTaskDto data = new ModifyCypherTaskDto(
            null,
            "MATCH (p:Person) RETURN p.name AS name",
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
            null);

        CypherTask task = invokeCreate(1L, modifyDto(data));
        assertEquals(CypherEvaluationMode.PENALTY, task.getEvaluationMode());
    }

    private static ModifyCypherTaskDto multiSolutionData(List<AlternativeSolutionDto> alternatives) {
        return new ModifyCypherTaskDto(
            CypherEvaluationMode.MULTI_SOLUTION,
            null,
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"),
            alternatives);
    }

    private static ModifyTaskDto<ModifyCypherTaskDto> modifyDto(ModifyCypherTaskDto data) {
        return new ModifyTaskDto<>(1L, BigDecimal.TEN, "cypher", TaskStatus.APPROVED, data);
    }

    private CypherTask invokeCreate(long id, ModifyTaskDto<ModifyCypherTaskDto> dto) {
        try {
            Method method = CypherTaskService.class.getDeclaredMethod("createTask", long.class, ModifyTaskDto.class);
            method.setAccessible(true);
            return (CypherTask) method.invoke(service, id, dto);
        } catch (ReflectiveOperationException ex) {
            if (ex.getCause() instanceof RuntimeException re)
                throw re;
            throw new RuntimeException(ex);
        }
    }

    private void invokeUpdate(CypherTask existing, ModifyTaskDto<ModifyCypherTaskDto> dto) {
        try {
            Method method = CypherTaskService.class.getDeclaredMethod("updateTask", CypherTask.class, ModifyTaskDto.class);
            method.setAccessible(true);
            method.invoke(service, existing, dto);
        } catch (ReflectiveOperationException ex) {
            if (ex.getCause() instanceof RuntimeException re)
                throw re;
            throw new RuntimeException(ex);
        }
    }
}
