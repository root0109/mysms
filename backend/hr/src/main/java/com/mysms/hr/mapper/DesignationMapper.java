package com.mysms.hr.mapper;

import com.mysms.hr.api.model.Designation;
import com.mysms.core.web.Etags;
import com.mysms.hr.domain.DesignationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps hr.designation to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = HrMapperConfig.class, imports = {Etags.class})
public interface DesignationMapper {

    @Mapping(target = "staffCategory", expression =
            "java(entity.getStaffCategory() == null ? null : "
                    + "Designation.StaffCategoryEnum.fromValue(entity.getStaffCategory()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Designation toDto(DesignationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "staffCategory", expression =
            "java(dto.getStaffCategory() == null ? null : dto.getStaffCategory().getValue())")
    DesignationEntity toEntity(Designation dto);
}
