package at.jku.dke.task_app.cypher.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskController;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.dto.CypherTaskDto;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskDto;
import at.jku.dke.task_app.cypher.services.CypherTaskService;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing {@link CypherTask}s.
 */
@RestController
public class TaskController extends BaseTaskController<CypherTask, CypherTaskDto, ModifyCypherTaskDto> {

    /**
     * Creates a new instance of class {@link TaskController}.
     *
     * @param taskService The task service.
     */
    public TaskController(CypherTaskService taskService) {
        super(taskService);
    }

    @Override
    protected CypherTaskDto mapToDto(CypherTask task) {
        return new CypherTaskDto(task.getSolution());
    }

}
