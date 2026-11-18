package com.mysms.attendance.mapper;

import com.mysms.attendance.domain.AttendanceRecordEntity;
import com.mysms.attendance.api.model.AttendanceRecord;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps attendance.attendance_record to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AttendanceMapperConfig.class, imports = {Etags.class})
public interface AttendanceRecordMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : AttendanceRecord.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AttendanceRecord toDto(AttendanceRecordEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "leaveRequestId", ignore = true)
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    @Mapping(target = "attendanceDate", ignore = true)
    AttendanceRecordEntity toEntity(AttendanceRecord dto);
}
