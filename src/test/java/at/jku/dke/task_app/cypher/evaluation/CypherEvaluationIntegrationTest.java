package at.jku.dke.task_app.cypher.evaluation;

import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.dto.TaskStatus;
import at.jku.dke.task_app.cypher.config.Neo4jProperties;
import at.jku.dke.task_app.cypher.data.entities.CypherEvaluationMode;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskRepository;
import at.jku.dke.task_app.cypher.dto.AlternativeSolutionDto;
import at.jku.dke.task_app.cypher.dto.CypherSubmissionDto;
import at.jku.dke.task_app.cypher.syntax.CypherStatementSplitter;
import at.jku.dke.task_app.cypher.syntax.CypherStatementValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class CypherEvaluationIntegrationTest {
    @Container
    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5").withoutAuthentication();

    private static final long PENALTY_TASK_ID = 1L;
    private static final long MULTI_TASK_ID = 2L;
    private static final long MULTI_UNSORTED_TASK_ID = 3L;

    private static Driver driver;
    private static EvaluationService evaluationService;

    @BeforeAll
    static void beforeAll() {
        driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.none());
        var splitter = new CypherStatementSplitter();
        var validator = new CypherStatementValidator(splitter);
        var properties = new Neo4jProperties();
        properties.setUri(NEO4J.getBoltUrl());
        properties.setUsername("neo4j");
        properties.setPassword("ignored");
        properties.setDatabase("neo4j");
        properties.setQueryTimeout(Duration.ofSeconds(15));
        properties.setMaxRows(100);
        var analyzer = new CypherAnalyzer(driver, properties, splitter, validator);

        var taskGroup = new CypherTaskGroup("""
            CREATE (:Person {name: 'Alice', age: 32});
            CREATE (:Person {name: 'Bob', age: 41});
            """,
            """
            CREATE (:Person {name: 'Carol', age: 25});
            CREATE (:Person {name: 'Dave', age: 38});
            """);
        var penaltyTask = new CypherTask(
            "MATCH (p:Person) RETURN p.name AS name ORDER BY name",
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100));
        penaltyTask.setId(PENALTY_TASK_ID);
        penaltyTask.setTaskGroup(taskGroup);
        penaltyTask.setMaxPoints(BigDecimal.TEN);
        penaltyTask.setStatus(TaskStatus.APPROVED);

        var multiTask = new CypherTask(
            "MATCH (p:Person) RETURN p.name AS name, p.age AS age",
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100));
        multiTask.setId(MULTI_TASK_ID);
        multiTask.setTaskGroup(taskGroup);
        multiTask.setMaxPoints(BigDecimal.TEN);
        multiTask.setStatus(TaskStatus.APPROVED);
        multiTask.setEvaluationMode(CypherEvaluationMode.MULTI_SOLUTION);
        multiTask.replaceAlternativeSolutions(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name, p.age AS age", new BigDecimal("100")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("60")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.age AS age", new BigDecimal("30"))
        ));

        var multiUnsortedTask = new CypherTask(
            "MATCH (p:Person) RETURN p.name AS name, p.age AS age",
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(100));
        multiUnsortedTask.setId(MULTI_UNSORTED_TASK_ID);
        multiUnsortedTask.setTaskGroup(taskGroup);
        multiUnsortedTask.setMaxPoints(BigDecimal.TEN);
        multiUnsortedTask.setStatus(TaskStatus.APPROVED);
        multiUnsortedTask.setEvaluationMode(CypherEvaluationMode.MULTI_SOLUTION);
        multiUnsortedTask.replaceAlternativeSolutions(List.of(
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name, p.age AS age", new BigDecimal("100")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("40")),
            new AlternativeSolutionDto("MATCH (p:Person) RETURN p.name AS name", new BigDecimal("60"))
        ));

        CypherTaskRepository repository = Mockito.mock(CypherTaskRepository.class);
        when(repository.findById(PENALTY_TASK_ID)).thenReturn(Optional.of(penaltyTask));
        when(repository.findById(MULTI_TASK_ID)).thenReturn(Optional.of(multiTask));
        when(repository.findById(MULTI_UNSORTED_TASK_ID)).thenReturn(Optional.of(multiUnsortedTask));

        var messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");
        evaluationService = new EvaluationService(repository, analyzer, messageSource);
    }

    @AfterAll
    static void afterAll() {
        if (driver != null)
            driver.close();
    }

    @Test
    void correctSubmissionEarnsMaxPoints() {
        var result = evaluationService.evaluate(submission(PENALTY_TASK_ID, "MATCH (p:Person) RETURN p.name AS name ORDER BY name", SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.TEN));
        assertEquals("Your solution is correct.", result.generalFeedback());
    }

    @Test
    void wrongRowsEarnsZeroPoints() {
        var result = evaluationService.evaluate(submission(PENALTY_TASK_ID, "MATCH (p:Person {name: 'Alice'}) RETURN p.name AS name", SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.ZERO));
        assertEquals("Your solution is incorrect.", result.generalFeedback());
        assertTrue(result.criteria().stream().anyMatch(criterion -> criterion.name().equals("Missing rows")));
    }

    @Test
    void wrongOrderingFailsWhenSolutionOrders() {
        var result = evaluationService.evaluate(submission(PENALTY_TASK_ID, "MATCH (p:Person) RETURN p.name AS name ORDER BY name DESC", SubmissionMode.DIAGNOSE));

        assertEquals(0, result.points().compareTo(BigDecimal.ZERO));
        assertTrue(result.criteria().stream().anyMatch(criterion -> criterion.name().equals("Order")));
    }

    @Test
    void mutatingSubmissionIsRejectedAndGraphIsRolledBack() {
        var result = evaluationService.evaluate(submission(PENALTY_TASK_ID, "CREATE (:Person {name: 'Mallory'}) RETURN 1", SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.ZERO));
        assertEquals("The analysis of the submitted query failed.", result.generalFeedback());
        assertEquals(0L, countPeopleInPersistentGraph());
    }

    @Test
    void hardcodedSubmissionMatchingPrimaryGraphFailsOnSecondary() {
        var result = evaluationService.evaluate(submission(PENALTY_TASK_ID,
            "RETURN 'Alice' AS name UNION ALL RETURN 'Bob' AS name",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.ZERO));
        assertEquals("Your solution is incorrect.", result.generalFeedback());
        assertEquals(2, result.criteria().size());
        assertTrue(result.criteria().stream().anyMatch(criterion ->
            criterion.name().equals("Result")
                && !criterion.passed()
                && criterion.feedback().equals("Your query does not produce the requested result.")));
    }

    @Test
    void successfulEvaluationRollsBackSetupGraph() {
        evaluationService.evaluate(submission(PENALTY_TASK_ID, "MATCH (p:Person) RETURN p.name AS name ORDER BY name", SubmissionMode.SUBMIT));

        assertEquals(0L, countPeopleInPersistentGraph());
    }

    @Test
    void multiSolutionTopStageEarnsFullPoints() {
        var result = evaluationService.evaluate(submission(MULTI_TASK_ID,
            "MATCH (p:Person) RETURN p.name AS name, p.age AS age",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.TEN));
        assertEquals("Your solution is correct.", result.generalFeedback());
    }

    @Test
    void multiSolutionMiddleStageEarnsPartialPoints() {
        var result = evaluationService.evaluate(submission(MULTI_TASK_ID,
            "MATCH (p:Person) RETURN p.name AS name",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(new BigDecimal("6.00")));
        assertEquals("Your solution matched a partial-credit stage.", result.generalFeedback());
        assertTrue(result.criteria().stream().noneMatch(c -> c.name().equals("Matched stage")));
        assertTrue(result.criteria().stream().noneMatch(c -> c.name().startsWith("Next stage")));
        assertTrue(result.criteria().stream().anyMatch(c -> c.name().equals("Return keys") && !c.passed()));
    }

    @Test
    void multiSolutionLowestStageEarnsLowestPoints() {
        var result = evaluationService.evaluate(submission(MULTI_TASK_ID,
            "MATCH (p:Person) RETURN p.age AS age",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(new BigDecimal("3.00")));
    }

    @Test
    void multiSolutionNoMatchEarnsZeroAndShowsDiffAgainstTopStage() {
        var result = evaluationService.evaluate(submission(MULTI_TASK_ID,
            "MATCH (p:Person) RETURN p.name AS qqq",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.ZERO));
        assertEquals("Your solution is incorrect.", result.generalFeedback());
        assertTrue(result.criteria().stream().anyMatch(c -> c.name().equals("Return keys") && !c.passed()));
        assertTrue(result.criteria().stream().noneMatch(c -> c.name().equals("Matched stage")));
        assertTrue(result.criteria().stream().noneMatch(c -> c.name().startsWith("Next stage")));
    }

    @Test
    void multiSolutionSyntaxErrorEarnsZero() {
        var result = evaluationService.evaluate(submission(MULTI_TASK_ID,
            "CREATE (:Person {name: 'Mallory'}) RETURN 1",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(BigDecimal.ZERO));
        assertEquals("The analysis of the submitted query failed.", result.generalFeedback());
    }

    @Test
    void multiSolutionReSortsByPointsSoHigherEquivalentStageWins() {
        var result = evaluationService.evaluate(submission(MULTI_UNSORTED_TASK_ID,
            "MATCH (p:Person) RETURN p.name AS name",
            SubmissionMode.SUBMIT));

        assertEquals(0, result.points().compareTo(new BigDecimal("6.00")));
        assertEquals("Your solution matched a partial-credit stage.", result.generalFeedback());
    }

    @Test
    void multiSolutionSuppressesDiffWhenStage1Matches() {
        var result = evaluationService.evaluate(submission(MULTI_TASK_ID,
            "MATCH (p:Person) RETURN p.name AS name, p.age AS age",
            SubmissionMode.SUBMIT));

        assertEquals(1, result.criteria().size());
        assertTrue(result.criteria().stream().noneMatch(c -> c.name().equals("Matched stage")));
        assertTrue(result.criteria().stream().noneMatch(c -> c.name().startsWith("Next stage")));
    }

    private static SubmitSubmissionDto<CypherSubmissionDto> submission(long taskId, String query, SubmissionMode mode) {
        return new SubmitSubmissionDto<>("u1", "a1", taskId, "en", mode, 3, new CypherSubmissionDto(query));
    }

    private static long countPeopleInPersistentGraph() {
        try (var session = driver.session()) {
            return session.executeRead(tx -> tx.run("MATCH (p:Person) RETURN count(p) AS c").single().get("c").asLong());
        }
    }
}
