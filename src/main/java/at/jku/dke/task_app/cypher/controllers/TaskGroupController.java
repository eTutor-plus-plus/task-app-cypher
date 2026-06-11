package at.jku.dke.task_app.cypher.controllers;

import at.jku.dke.etutor.task_app.controllers.BaseTaskGroupController;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskGroupDto;
import at.jku.dke.task_app.cypher.dto.CypherTaskGroupDto;
import at.jku.dke.task_app.cypher.services.CypherTaskGroupService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskGroupController extends BaseTaskGroupController<CypherTaskGroup, CypherTaskGroupDto, ModifyCypherTaskGroupDto> {

    public TaskGroupController(CypherTaskGroupService taskGroupService) {
        super(taskGroupService);
    }

    @Override
    protected CypherTaskGroupDto mapToDto(CypherTaskGroup taskGroup) {
        return new CypherTaskGroupDto(taskGroup.getSetupStatements(), taskGroup.getSecondarySetupStatements());
    }

}
