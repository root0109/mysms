package com.mysms.attendance.mapper;

import com.mysms.attendance.domain.AttendanceSheetEntity;
import com.mysms.attendance.api.model.AttendanceSheet;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for attendance.attendance_sheet. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AttendanceMapperConfig.class, imports = {Etags.class})
public interface AttendanceSheetMapper {

    @Mapping(target = "dayPart", expression =
            "java(entity.getDayPart() == null ? null : AttendanceSheet.DayPartEnum.fromValue(entity.getDayPart()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : AttendanceSheet.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AttendanceSheet toDto(AttendanceSheetEntity entity);

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
    AttendanceSheetEntity toEntity(AttendanceSheet dto);
}
