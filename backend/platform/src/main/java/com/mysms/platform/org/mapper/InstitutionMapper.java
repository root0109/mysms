package com.mysms.platform.org.mapper;

import com.mysms.platform.org.api.model.Institution;
import com.mysms.core.web.Etags;
import com.mysms.platform.org.domain.InstitutionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for org.institution. The way in ignores the id, tenant, audit set and read-only fields, so a
 * request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = OrgMapperConfig.class, imports = {Etags.class})
public interface InstitutionMapper {

    @Mapping(target = "institutionType", expression =
            "java(entity.getInstitutionType() == null ? null : "
                    + "Institution.InstitutionTypeEnum.fromValue(entity.getInstitutionType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Institution.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Institution toDto(InstitutionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "institutionType", expression =
            "java(dto.getInstitutionType() == null ? null : dto.getInstitutionType().getValue())")
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    InstitutionEntity toEntity(Institution dto);
}
