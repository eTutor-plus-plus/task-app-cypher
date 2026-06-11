package at.jku.dke.task_app.cypher.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskController;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskAlternativeSolution;
import at.jku.dke.task_app.cypher.dto.AlternativeSolutionDto;
import at.jku.dke.task_app.cypher.dto.CypherTaskDto;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskDto;
import at.jku.dke.task_app.cypher.services.CypherTaskService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TaskController extends BaseTaskController<CypherTask, CypherTaskDto, ModifyCypherTaskDto> {

    public TaskController(CypherTaskService taskService) {
        super(taskService);
    }

    @Override
    protected CypherTaskDto mapToDto(CypherTask task) {
        List<AlternativeSolutionDto> alternatives = task.getAlternativeSolutions().stream()
            .map(this::toDto)
            .toList();
        return new CypherTaskDto(
            task.getEvaluationMode(),
            task.getSolution(),
            task.getSuperfluousColumnsPenalty(),
            task.getMissingRowsPenalty(),
            task.getSuperfluousRowsPenalty(),
            task.getWrongOrderPenalty(),
            task.getExpectedColumnNames(),
            alternatives);
    }

    private AlternativeSolutionDto toDto(CypherTaskAlternativeSolution entity) {
        return new AlternativeSolutionDto(entity.getSolution(), entity.getPointsPercent());
    }
}
