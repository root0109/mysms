package com.mysms.hr.mapper;

import com.mysms.hr.api.model.LeaveApplication;
import com.mysms.core.web.Etags;
import com.mysms.hr.domain.LeaveApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps hr.leave_application to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = HrMapperConfig.class, imports = {Etags.class})
public interface LeaveApplicationMapper {

    @Mapping(target = "dayPart", expression =
            "java(entity.getDayPart() == null ? null : LeaveApplication.DayPartEnum.fromValue(entity.getDayPart()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : LeaveApplication.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LeaveApplication toDto(LeaveApplicationEntity entity);

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
    @Mapping(target = "decidedByUserId", ignore = true)
    @Mapping(target = "decidedOn", ignore = true)
    @Mapping(target = "decisionComments", ignore = true)
    LeaveApplicationEntity toEntity(LeaveApplication dto);
}
