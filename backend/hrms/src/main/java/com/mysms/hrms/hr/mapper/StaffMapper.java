package com.mysms.hrms.hr.mapper;

import com.mysms.hrms.hr.api.model.Staff;
import com.mysms.core.web.Etags;
import com.mysms.hrms.hr.domain.StaffEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps hr.staff to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = HrMapperConfig.class, imports = {Etags.class})
public interface StaffMapper {

    @Mapping(target = "employmentType", expression =
            "java(entity.getEmploymentType() == null ? null : "
                    + "Staff.EmploymentTypeEnum.fromValue(entity.getEmploymentType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Staff.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Staff toDto(StaffEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "employmentType", expression =
            "java(dto.getEmploymentType() == null ? null : dto.getEmploymentType().getValue())")
    @Mapping(target = "status", ignore = true)
    StaffEntity toEntity(Staff dto);
}
