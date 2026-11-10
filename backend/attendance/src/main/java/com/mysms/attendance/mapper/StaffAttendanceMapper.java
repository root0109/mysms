package com.mysms.attendance.mapper;

import com.mysms.attendance.domain.StaffAttendanceEntity;
import com.mysms.attendance.api.model.StaffAttendance;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for attendance.staff_attendance. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AttendanceMapperConfig.class, imports = {Etags.class})
public interface StaffAttendanceMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : StaffAttendance.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "source", expression =
            "java(entity.getSource() == null ? null : StaffAttendance.SourceEnum.fromValue(entity.getSource()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StaffAttendance toDto(StaffAttendanceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "recordedByUserId", ignore = true)
    StaffAttendanceEntity toEntity(StaffAttendance dto);
}
