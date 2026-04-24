package at.jku.dke.task_app.cypher.services;

import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.etutor.task_app.services.BaseSubmissionService;
import at.jku.dke.task_app.cypher.data.entities.CypherSubmission;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.repositories.CypherSubmissionRepository;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskRepository;
import at.jku.dke.task_app.cypher.dto.CypherSubmissionDto;
import at.jku.dke.task_app.cypher.evaluation.EvaluationService;
import org.springframework.stereotype.Service;

/**
 * This class provides methods for managing {@link CypherSubmission}s.
 */
@Service
public class CypherSubmissionService extends BaseSubmissionService<CypherTask, CypherSubmission, CypherSubmissionDto> {

    private final EvaluationService evaluationService;

    /**
     * Creates a new instance of class {@link CypherSubmissionService}.
     *
     * @param submissionRepository The input repository.
     * @param taskRepository       The task repository.
     * @param evaluationService    The evaluation service.
     */
    public CypherSubmissionService(CypherSubmissionRepository submissionRepository, CypherTaskRepository taskRepository, EvaluationService evaluationService) {
        super(submissionRepository, taskRepository);
        this.evaluationService = evaluationService;
    }

    @Override
    protected CypherSubmission createSubmissionEntity(SubmitSubmissionDto<CypherSubmissionDto> submitSubmissionDto) {
        return new CypherSubmission(submitSubmissionDto.submission().input());
    }

    @Override
    protected GradingDto evaluate(SubmitSubmissionDto<CypherSubmissionDto> submitSubmissionDto) {
        return this.evaluationService.evaluate(submitSubmissionDto);
    }

    @Override
    protected CypherSubmissionDto mapSubmissionToSubmissionData(CypherSubmission submission) {
        return new CypherSubmissionDto(submission.getSubmission());
    }

}
