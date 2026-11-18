package com.mysms.academics.mapper;

import com.mysms.academics.domain.ClassSubjectEntity;
import com.mysms.academics.api.model.ClassSubject;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for academics.class_subject. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = AcademicsMapperConfig.class, imports = {Etags.class})
public interface ClassSubjectMapper {

    @Mapping(target = "offeringType", expression =
            "java(entity.getOfferingType() == null ? null : "
                    + "ClassSubject.OfferingTypeEnum.fromValue(entity.getOfferingType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ClassSubject toDto(ClassSubjectEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "classSectionId", ignore = true)
    @Mapping(target = "offeringType", expression =
            "java(dto.getOfferingType() == null ? null : dto.getOfferingType().getValue())")
    ClassSubjectEntity toEntity(ClassSubject dto);
}
