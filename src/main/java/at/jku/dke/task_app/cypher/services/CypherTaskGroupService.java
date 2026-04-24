package at.jku.dke.task_app.cypher.services;

import at.jku.dke.etutor.task_app.dto.ModifyTaskGroupDto;
import at.jku.dke.etutor.task_app.dto.TaskGroupModificationResponseDto;
import at.jku.dke.etutor.task_app.services.BaseTaskGroupService;
import at.jku.dke.task_app.cypher.data.entities.CypherTaskGroup;
import at.jku.dke.task_app.cypher.data.repositories.CypherTaskGroupRepository;
import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskGroupDto;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * This class provides methods for managing {@link CypherTaskGroup}s.
 */
@Service
public class CypherTaskGroupService extends BaseTaskGroupService<CypherTaskGroup, ModifyCypherTaskGroupDto> {

    private final MessageSource messageSource;

    /**
     * Creates a new instance of class {@link CypherTaskGroupService}.
     *
     * @param repository    The task group repository.
     * @param messageSource The message source.
     */
    public CypherTaskGroupService(CypherTaskGroupRepository repository, MessageSource messageSource) {
        super(repository);
        this.messageSource = messageSource;
    }

    @Override
    protected CypherTaskGroup createTaskGroup(long id, ModifyTaskGroupDto<ModifyCypherTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        return new CypherTaskGroup(modifyTaskGroupDto.additionalData().minNumber(), modifyTaskGroupDto.additionalData().maxNumber());
    }

    @Override
    protected void updateTaskGroup(CypherTaskGroup taskGroup, ModifyTaskGroupDto<ModifyCypherTaskGroupDto> modifyTaskGroupDto) {
        if (!modifyTaskGroupDto.taskGroupType().equals("cypher"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid task group type.");
        taskGroup.setMinNumber(modifyTaskGroupDto.additionalData().minNumber());
        taskGroup.setMaxNumber(modifyTaskGroupDto.additionalData().maxNumber());
    }

    @Override
    protected TaskGroupModificationResponseDto mapToReturnData(CypherTaskGroup taskGroup, boolean create) {
        return new TaskGroupModificationResponseDto(
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getMinNumber(), taskGroup.getMaxNumber()}, Locale.GERMAN),
            this.messageSource.getMessage("defaultTaskGroupDescription", new Object[]{taskGroup.getMinNumber(), taskGroup.getMaxNumber()}, Locale.ENGLISH));
    }
}
