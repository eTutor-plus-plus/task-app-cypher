package at.jku.dke.task_app.cypher.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseSubmissionController;
import at.jku.dke.task_app.cypher.data.entities.CypherSubmission;
import at.jku.dke.task_app.cypher.dto.CypherSubmissionDto;
import at.jku.dke.task_app.cypher.services.CypherSubmissionService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubmissionController extends BaseSubmissionController<CypherSubmissionDto> {
    public SubmissionController(CypherSubmissionService submissionService) {
        super(submissionService);
    }
}
