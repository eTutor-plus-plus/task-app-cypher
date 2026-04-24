package at.jku.dke.task_app.cypher.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskDto;
import at.jku.dke.etutor.task_app.dto.TaskModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskInGroupService;
import at.jku.dke.task_app.cypher.data.entities.CypherTask;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskGroupRepository;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskRepository;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * This class provides methods for managing {@link CypherTask}s.
 */
@Service
public class CypherTaskService extends BaseTaskInGroupService<CypherTask, CypherTaskGroup, ModifyCypherTaskDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link CypherTaskService}.
     *
     * @param repository          The task repository.
     * @param taskGroupRepository The task group repository.
     * @param messageSource       The message source.
     */
    public CypherTaskService(CypherTaskRepository repository, CypherTaskGroupRepository taskGroupRepository, MessageSource messageSource) {
        super(repository, taskGroupRepository);
        this.messageSource = messageSource;
    }

    @Override
    protected CypherTask createTask(long id, ModifyTaskDto<ModifyCypherTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        return new CypherTask(modifyTaskDto.additionalData().solution());
    }

    @Override
    protected void updateTask(CypherTask task, ModifyTaskDto<ModifyCypherTaskDto> modifyTaskDto) {
        if (!modifyTaskDto.taskType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task type.");
        task.setSolution(modifyTaskDto.additionalData().solution());
    }

    @Override
    protected TaskModificationResponseDto mapToReturnData(CypherTask task, boolean create) {
        return new TaskModificationResponseDto(
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskDescription", null, Locale.ENGLISH)
        );
    }
}
