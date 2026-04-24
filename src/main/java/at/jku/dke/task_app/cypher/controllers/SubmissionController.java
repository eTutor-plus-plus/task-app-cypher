package at.jku.dke.task_app.cypher.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseSubmissionController;
import at.jku.dke.task_app.cypher.data.entities.CypherSubmission;
import at.jku.dke.task_app.cypher.dto.CypherSubmissionDto;
import at.jku.dke.task_app.cypher.services.CypherSubmissionService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link CypherSubmission}s.
 */
@RestController
public class SubmissionController extends BaseSubmissionController<CypherSubmissionDto> {
    /**
     * Creates a new instance of class {@link SubmissionController}.
     *
     * @param submissionService The input service.
     */
    public SubmissionController(CypherSubmissionService submissionService) {
        super(submissionService);
    }
}
