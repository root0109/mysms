package com.mysms.comms.mapper;

import com.mysms.comms.domain.TaskEntity;
import com.mysms.comms.api.model.Task;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for comms.task. The way in ignores the id, the tenant, the audit set and every read-only field,
 * so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface TaskMapper {

    @Mapping(target = "taskCategory", expression =
            "java(entity.getTaskCategory() == null ? null : Task.TaskCategoryEnum.fromValue(entity.getTaskCategory()))")
    @Mapping(target = "reminderOption", expression =
            "java(entity.getReminderOption() == null ? null : "
                    + "Task.ReminderOptionEnum.fromValue(entity.getReminderOption()))")
    @Mapping(target = "priority", expression =
            "java(entity.getPriority() == null ? null : Task.PriorityEnum.fromValue(entity.getPriority()))")
    @Mapping(target = "assignmentMode", expression =
            "java(entity.getAssignmentMode() == null ? null : "
                    + "Task.AssignmentModeEnum.fromValue(entity.getAssignmentMode()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Task toDto(TaskEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "taskCategory", expression =
            "java(dto.getTaskCategory() == null ? null : dto.getTaskCategory().getValue())")
    @Mapping(target = "reminderOption", expression =
            "java(dto.getReminderOption() == null ? null : dto.getReminderOption().getValue())")
    @Mapping(target = "isCompleted", ignore = true)
    @Mapping(target = "isSnoozed", ignore = true)
    @Mapping(target = "priority", expression = "java(dto.getPriority() == null ? null : dto.getPriority().getValue())")
    @Mapping(target = "assignmentMode", expression =
            "java(dto.getAssignmentMode() == null ? null : dto.getAssignmentMode().getValue())")
    TaskEntity toEntity(Task dto);
}
