package com.mysms.hrms.hr.mapper;

import com.mysms.hrms.hr.api.model.LeaveType;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.hrms.hr.domain.LeaveTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps hr.leave_type to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = HrMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LeaveTypeMapper {

    @Mapping(target = "maxDaysPerYear", expression = "java(WireValues.decimal(entity.getMaxDaysPerYear()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LeaveType toDto(LeaveTypeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "maxDaysPerYear", expression = "java(WireValues.decimal(dto.getMaxDaysPerYear()))")
    LeaveTypeEntity toEntity(LeaveType dto);
}
