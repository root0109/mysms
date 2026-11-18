package com.mysms.comms.mapper;

import com.mysms.comms.domain.TaskAssigneeEntity;
import com.mysms.comms.api.model.TaskAssignee;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.task_assignee to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface TaskAssigneeMapper {

    @Mapping(target = "assigneeType", expression =
            "java(entity.getAssigneeType() == null ? null : "
                    + "TaskAssignee.AssigneeTypeEnum.fromValue(entity.getAssigneeType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : TaskAssignee.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    TaskAssignee toDto(TaskAssigneeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "assigneeType", expression =
            "java(dto.getAssigneeType() == null ? null : dto.getAssigneeType().getValue())")
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    TaskAssigneeEntity toEntity(TaskAssignee dto);
}
