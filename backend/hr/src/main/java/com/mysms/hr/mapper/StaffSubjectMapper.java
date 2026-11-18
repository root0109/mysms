package com.mysms.hr.mapper;

import com.mysms.hr.api.model.StaffSubject;
import com.mysms.core.web.Etags;
import com.mysms.hr.domain.StaffSubjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps hr.staff_subject to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = HrMapperConfig.class, imports = {Etags.class})
public interface StaffSubjectMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StaffSubject toDto(StaffSubjectEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "staffId", ignore = true)
    StaffSubjectEntity toEntity(StaffSubject dto);
}
