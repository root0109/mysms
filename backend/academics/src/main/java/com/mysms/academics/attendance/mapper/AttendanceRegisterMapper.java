package com.mysms.academics.attendance.mapper;

import com.mysms.academics.attendance.domain.AttendanceRegisterEntity;
import com.mysms.academics.attendance.api.model.AttendanceRegister;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps attendance.attendance_register to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AttendanceMapperConfig.class, imports = {Etags.class})
public interface AttendanceRegisterMapper {

    @Mapping(target = "registerScope", expression =
            "java(entity.getRegisterScope() == null ? null : "
                    + "AttendanceRegister.RegisterScopeEnum.fromValue(entity.getRegisterScope()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AttendanceRegister toDto(AttendanceRegisterEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "registerScope", expression =
            "java(dto.getRegisterScope() == null ? null : dto.getRegisterScope().getValue())")
    AttendanceRegisterEntity toEntity(AttendanceRegister dto);
}
