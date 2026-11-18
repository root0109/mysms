package com.mysms.attendance.mapper;

import com.mysms.attendance.domain.AbsenceReasonEntity;
import com.mysms.attendance.api.model.AbsenceReason;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps attendance.absence_reason to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AttendanceMapperConfig.class, imports = {Etags.class})
public interface AbsenceReasonMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AbsenceReason toDto(AbsenceReasonEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    AbsenceReasonEntity toEntity(AbsenceReason dto);
}
