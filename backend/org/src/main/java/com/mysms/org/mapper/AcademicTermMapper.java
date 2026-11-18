package com.mysms.org.mapper;

import com.mysms.org.api.model.AcademicTerm;
import com.mysms.core.web.Etags;
import com.mysms.org.domain.AcademicTermEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for org.academic_term. The way in ignores the id, tenant, audit set and read-only fields, so a
 * request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = OrgMapperConfig.class, imports = {Etags.class})
public interface AcademicTermMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AcademicTerm toDto(AcademicTermEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "academicYearId", ignore = true)
    AcademicTermEntity toEntity(AcademicTerm dto);
}
