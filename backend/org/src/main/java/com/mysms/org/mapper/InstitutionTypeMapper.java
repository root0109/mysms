package com.mysms.org.mapper;

import com.mysms.org.api.model.InstitutionType;
import com.mysms.core.web.Etags;
import com.mysms.org.domain.InstitutionTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for org.institution_type. The way in ignores the id, tenant, audit set and read-only fields, so
 * a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = OrgMapperConfig.class, imports = {Etags.class})
public interface InstitutionTypeMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    InstitutionType toDto(InstitutionTypeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    InstitutionTypeEntity toEntity(InstitutionType dto);
}
