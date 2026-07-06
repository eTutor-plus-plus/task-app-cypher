package at.jku.dke.task_app.cypher.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.cypher.data.entities.CypherEvaluationMode;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskAlternativeSolution;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskRepository;
import at.jku.dke.task_app.cypher.dto.CypherSubmissionDto;
import at.jku.dke.task_app.cypher.evaluation.model.CypherComparison;
import at.jku.dke.task_app.cypher.evaluation.model.CypherEvaluationCriterion;
import at.jku.dke.task_app.cypher.evaluation.model.CypherGrading;
import at.jku.dke.task_app.cypher.evaluation.model.CypherQueryResult;
import at.jku.dke.task_app.cypher.exception.CypherResultLimitExceededException;
import at.jku.dke.task_app.cypher.exception.CypherValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EvaluationService {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CypherTaskRepository taskRepository;
    private final CypherAnalyzer analyzer;
    private final MessageSource messageSource;

    public EvaluationService(CypherTaskRepository taskRepository, CypherAnalyzer analyzer, MessageSource messageSource) {
        this.taskRepository = taskRepository;
        this.analyzer = analyzer;
        this.messageSource = messageSource;
    }

    @Transactional
    public GradingDto evaluate(SubmitSubmissionDto<CypherSubmissionDto> submission) {
        var task = this.taskRepository.findById(submission.taskId()).orElseThrow(() -> new EntityNotFoundException("Task " + submission.taskId() + " does not exist."));

        LOG.info("Evaluating Cypher query for task {} with mode {} and feedback-level {} ({} alternatives)",
            submission.taskId(), submission.mode(), submission.feedbackLevel(), task.getAlternativeSolutions().size());
        Locale locale = Locale.of(submission.language());
       String setupStatements = submission.mode() == SubmissionMode.SUBMIT
            ? task.getTaskGroup().getSecondarySetupStatements()
            : task.getTaskGroup().getSetupStatements();
        if (setupStatements == null || setupStatements.isBlank())
            setupStatements = task.getTaskGroup().getSetupStatements();
        String submittedQuery = submission.submission().input();

        if (submission.mode() == SubmissionMode.RUN)
            return this.run(task.getMaxPoints(), setupStatements, submittedQuery, locale);

        if (task.getEvaluationMode() == CypherEvaluationMode.MULTI_SOLUTION)
            return this.evaluateMultiSolution(task, setupStatements, submittedQuery, submission.mode(), submission.feedbackLevel(), locale);
        return this.evaluatePenalty(task, setupStatements, submittedQuery, submission.mode(), submission.feedbackLevel(), locale);
    }

    private GradingDto evaluatePenalty(CypherTask task, String setupStatements,
                                       String submittedQuery, SubmissionMode mode, int feedbackLevel, Locale locale) {
        CypherComparison comparison;
        try {
            comparison = this.analyzer.compare(setupStatements, task.getSolution(), submittedQuery);
        } catch (CypherValidationException | CypherResultLimitExceededException | Neo4jException ex) {
            return this.failed(task.getMaxPoints(), CypherGrading.forSyntaxError(task).getPoints(), locale, ex);
        }

        // A near-matching column name (typo hint) counts as a syntax error; the submission is not graded.
        if (!comparison.nearMatches().isEmpty())
            return this.typoSyntaxError(task.getMaxPoints(), comparison, locale);

        CypherGrading grading = CypherGrading.of(task, comparison);
        String feedback = this.messageSource.getMessage(
            grading.isCorrect() ? (mode == SubmissionMode.SUBMIT ? "correct" : "possiblyCorrect") : "incorrect",
            null,
            locale);
        List<CriterionDto> criteria = this.createComparisonCriteria(comparison, mode, feedbackLevel, grading, locale);
        return new GradingDto(task.getMaxPoints(), grading.getPoints(), feedback, criteria);
    }

    private GradingDto evaluateMultiSolution(CypherTask task, String setupStatements,
                                             String submittedQuery, SubmissionMode mode, int feedbackLevel, Locale locale) {
        List<CypherTaskAlternativeSolution> alternatives = new ArrayList<>(task.getAlternativeSolutions());
        alternatives.sort(Comparator.comparing(CypherTaskAlternativeSolution::getPointsPercent).reversed());
        if (alternatives.isEmpty()) {
            LOG.warn("Task {} is in MULTI_SOLUTION mode but has no alternatives configured.", task.getId());
            return this.failed(task.getMaxPoints(), BigDecimal.ZERO, locale, new IllegalStateException("No alternative solutions configured."));
        }

        List<CypherComparison> comparisons = new ArrayList<>(alternatives.size());
        int matchedIndex = -1;
        Exception firstFailure = null;
        for (int i = 0; i < alternatives.size(); i++) {
            CypherTaskAlternativeSolution alt = alternatives.get(i);
            CypherComparison comparison;
            try {
                comparison = this.analyzer.compare(setupStatements, alt.getSolution(), submittedQuery);
            } catch (CypherValidationException | CypherResultLimitExceededException | Neo4jException ex) {
                if (firstFailure == null)
                    firstFailure = ex;
                comparisons.add(null);
                continue;
            }
            comparisons.add(comparison);
            if (comparison.isCorrect()) {
                matchedIndex = i;
                break;
            }
        }

        if (matchedIndex < 0 && comparisons.stream().allMatch(c -> c == null) && firstFailure != null) {
            return this.failed(task.getMaxPoints(), BigDecimal.ZERO, locale, (RuntimeException) firstFailure);
        }

        if (matchedIndex < 0) {
            CypherComparison typo = comparisons.stream()
                .filter(c -> c != null && !c.nearMatches().isEmpty())
                .findFirst()
                .orElse(null);
            if (typo != null)
                return this.typoSyntaxError(task.getMaxPoints(), typo, locale);
        }

        BigDecimal points = matchedIndex >= 0
            ? task.getMaxPoints().multiply(alternatives.get(matchedIndex).getPointsPercent()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        boolean perfectMatch = matchedIndex >= 0 && alternatives.get(matchedIndex).getPointsPercent().compareTo(HUNDRED) == 0;
        String feedbackKey = matchedIndex < 0
            ? "incorrect"
            : (perfectMatch ? (mode == SubmissionMode.SUBMIT ? "correct" : "possiblyCorrect") : "multi.partiallyCorrect");
        String feedback = this.messageSource.getMessage(feedbackKey, null, locale);

        List<CriterionDto> criteria = this.createMultiSolutionCriteria(alternatives, comparisons, matchedIndex, mode, feedbackLevel, locale);
        return new GradingDto(task.getMaxPoints(), points, feedback, criteria);
    }

    private List<CriterionDto> createMultiSolutionCriteria(List<CypherTaskAlternativeSolution> alternatives,
                                                            List<CypherComparison> comparisons, int matchedIndex,
                                                            SubmissionMode mode, int feedbackLevel, Locale locale) {
        List<CriterionDto> criteria = new ArrayList<>();
        criteria.add(this.syntaxCriterion(locale, true, this.messageSource.getMessage("criterium.syntax.valid", null, locale)));

        boolean perfectMatch = matchedIndex >= 0 && alternatives.get(matchedIndex).getPointsPercent().compareTo(HUNDRED) == 0;
        if (feedbackLevel >= 2 && !perfectMatch) {
            int diffStage = matchedIndex > 0 ? matchedIndex - 1 : 0;
            CypherComparison diff = comparisons.size() > diffStage ? comparisons.get(diffStage) : null;
            if (diff != null)
                appendComparisonDetails(criteria, diff, feedbackLevel, mode, locale, Map.of());
        }

        if (mode != SubmissionMode.SUBMIT) {
            CypherComparison any = comparisons.stream().filter(c -> c != null).findFirst().orElse(null);
            if (any != null)
                this.appendResultTable(criteria, any.submissionResult(), perfectMatch, locale);
        }

        return criteria;
    }

    private void appendComparisonDetails(List<CriterionDto> target, CypherComparison comparison, int feedbackLevel,
                                          SubmissionMode mode, Locale locale, Map<CypherEvaluationCriterion, BigDecimal> deductions) {
        if (!comparison.keysCorrect()) {
            this.addCriterion(target, "criterium.columns",
                negatedDeduction(deductions, CypherEvaluationCriterion.CORRECT_COLUMNS, mode),
                false, this.createColumnFeedback(comparison, locale), locale);
            return;
        }

        boolean rowDiff = !comparison.missingRows().isEmpty() || !comparison.superfluousRows().isEmpty();
        if (feedbackLevel >= 2 && rowDiff)
            this.addCriterion(target, "criterium.rows.count", null, true,
                this.messageSource.getMessage("criterium.rows.count.text",
                    new Object[]{comparison.expectedRowCount(), comparison.actualRowCount()}, locale),
                locale);

        if (!comparison.missingRows().isEmpty())
            this.addCriterion(target, "criterium.rows.missing",
                negatedDeduction(deductions, CypherEvaluationCriterion.MISSING_ROWS, mode),
                false, this.createRowsFeedback("missing", comparison.missingRows(), comparison.keys(), feedbackLevel, locale), locale);
        if (!comparison.superfluousRows().isEmpty())
            this.addCriterion(target, "criterium.rows.superfluous",
                negatedDeduction(deductions, CypherEvaluationCriterion.SUPERFLUOUS_ROWS, mode),
                false, this.createRowsFeedback("superfluous", comparison.superfluousRows(), comparison.keys(), feedbackLevel, locale), locale);
        if (comparison.orderRelevant() && !comparison.orderCorrect())
            this.addCriterion(target, "criterium.order",
                negatedDeduction(deductions, CypherEvaluationCriterion.CORRECT_ORDER, mode),
                false, this.messageSource.getMessage(
                    comparison.rowsCorrect() ? "criterium.order.invalid" : "criterium.order.invalidRows", null, locale), locale);
    }

    private GradingDto run(BigDecimal maxPoints, String setupStatements, String submittedQuery, Locale locale) {
        try {
            CypherQueryResult result = this.analyzer.executeRead(setupStatements, submittedQuery);
            List<CriterionDto> criteria = new ArrayList<>();
            criteria.add(this.syntaxCriterion(locale, true, this.messageSource.getMessage("criterium.syntax.valid", null, locale)));
            criteria.add(new CriterionDto(
                this.messageSource.getMessage("criterium.result", null, locale),
                null,
                true,
                HtmlTableRenderer.render(result)));
            return new GradingDto(maxPoints, BigDecimal.ZERO, this.messageSource.getMessage("noSyntaxError", null, locale), criteria);
        } catch (CypherValidationException | CypherResultLimitExceededException | Neo4jException ex) {
            return this.failed(maxPoints, BigDecimal.ZERO, locale, ex);
        }
    }

    private List<CriterionDto> createComparisonCriteria(CypherComparison comparison, SubmissionMode mode, int feedbackLevel, CypherGrading grading, Locale locale) {
        List<CriterionDto> criteria = new ArrayList<>();
        criteria.add(this.syntaxCriterion(locale, true, this.messageSource.getMessage("criterium.syntax.valid", null, locale)));

        if (feedbackLevel > 0) {
            Map<CypherEvaluationCriterion, BigDecimal> deductions = new EnumMap<>(CypherEvaluationCriterion.class);
            for (CypherGrading.Entry entry : grading.getDetails())
                deductions.put(entry.criterion(), entry.minusPoints());
            this.appendComparisonDetails(criteria, comparison, feedbackLevel, mode, locale, deductions);
        }

        if (mode != SubmissionMode.SUBMIT)
            this.appendResultTable(criteria, comparison.submissionResult(), grading.isCorrect(), locale);

        return criteria;
    }

    private void appendResultTable(List<CriterionDto> criteria, CypherQueryResult result, boolean passed, Locale locale) {
        criteria.add(new CriterionDto(
            this.messageSource.getMessage("criterium.result", null, locale),
            null,
            passed,
            HtmlTableRenderer.render(result)));
    }

    private void addCriterion(List<CriterionDto> criteria, String key, BigDecimal points, boolean passed, String feedback, Locale locale) {
        criteria.add(new CriterionDto(this.messageSource.getMessage(key, null, locale), points, passed, feedback));
    }

    private static BigDecimal negatedDeduction(Map<CypherEvaluationCriterion, BigDecimal> deductions, CypherEvaluationCriterion criterion, SubmissionMode mode) {
        if (mode != SubmissionMode.SUBMIT)
            return null;
        BigDecimal d = deductions.get(criterion);
        return d == null ? null : d.negate();
    }

    private GradingDto typoSyntaxError(BigDecimal maxPoints, CypherComparison comparison, Locale locale) {
        List<CriterionDto> criteria = new ArrayList<>();
        criteria.add(this.syntaxCriterion(locale, false,
            this.messageSource.getMessage("criterium.syntax.invalid", null, locale) + "<br>" + this.createTypoFeedback(comparison, locale)));
        return new GradingDto(maxPoints, BigDecimal.ZERO, this.messageSource.getMessage("syntaxError", null, locale), criteria);
    }

    private String createTypoFeedback(CypherComparison comparison, Locale locale) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : comparison.nearMatches().entrySet()) {
            if (!builder.isEmpty())
                builder.append("<br>");
            builder.append(this.messageSource.getMessage("criterium.columns.typo.entry",
                new Object[]{entry.getValue(), entry.getKey()}, locale));
        }
        return builder.toString();
    }

    private String createColumnFeedback(CypherComparison comparison, Locale locale) {
        StringBuilder builder = new StringBuilder(this.messageSource.getMessage("criterium.columns.invalid", null, locale));
        if (!comparison.missingKeys().isEmpty()) {
            builder.append("<br>");
            builder.append(this.messageSource.getMessage("criterium.columns.missing", new Object[]{String.join(", ", comparison.missingKeys())}, locale));
        }
        if (!comparison.superfluousKeys().isEmpty()) {
            builder.append("<br>");
            builder.append(this.messageSource.getMessage("criterium.columns.superfluous", new Object[]{String.join(", ", comparison.superfluousKeys())}, locale));
        }
        return builder.toString();
    }

    private String createRowsFeedback(String suffix, List<List<String>> rows, List<String> keys, int feedbackLevel, Locale locale) {
        String base = "criterium.rows." + suffix;
        if (feedbackLevel <= 1)
            return this.messageSource.getMessage(base + ".invalid", null, locale);
        if (feedbackLevel == 2)
            return this.messageSource.getMessage(base + "Count", new Object[]{rows.size()}, locale);
        return this.messageSource.getMessage(base + "List",
            new Object[]{rows.size(), HtmlTableRenderer.render(keys, rows)}, locale);
    }

    private GradingDto failed(BigDecimal maxPoints, BigDecimal points, Locale locale, RuntimeException ex) {
        List<CriterionDto> criteria = new ArrayList<>();
        criteria.add(this.syntaxCriterion(locale, false, this.messageSource.getMessage("criterium.syntax.invalid", null, locale)));
        criteria.add(new CriterionDto(
            this.messageSource.getMessage("criterium.result", null, locale),
            null,
            false,
            "<strong>" + HtmlTableRenderer.escape(ex.getMessage()) + "</strong>"));
        return new GradingDto(maxPoints, points, this.messageSource.getMessage("executionFailed", null, locale), criteria);
    }

    private CriterionDto syntaxCriterion(Locale locale, boolean passed, String feedback) {
        return new CriterionDto(
            this.messageSource.getMessage("criterium.syntax", null, locale),
            null,
            passed,
            feedback);
    }
}
