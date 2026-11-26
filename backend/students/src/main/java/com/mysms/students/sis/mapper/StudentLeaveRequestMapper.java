package com.mysms.students.sis.mapper;

import com.mysms.students.sis.api.model.StudentLeaveRequest;
import com.mysms.core.web.Etags;
import com.mysms.students.sis.domain.StudentLeaveRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for sis.student_leave_request. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = SisMapperConfig.class, imports = {Etags.class})
public interface StudentLeaveRequestMapper {

    @Mapping(target = "dayPart", expression =
            "java(entity.getDayPart() == null ? null : StudentLeaveRequest.DayPartEnum.fromValue(entity.getDayPart()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : StudentLeaveRequest.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StudentLeaveRequest toDto(StudentLeaveRequestEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "dayPart", expression = "java(dto.getDayPart() == null ? null : dto.getDayPart().getValue())")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requestedByUserId", ignore = true)
    @Mapping(target = "decidedByUserId", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    @Mapping(target = "decisionNote", ignore = true)
    StudentLeaveRequestEntity toEntity(StudentLeaveRequest dto);
}
