package com.mysms.students.sis.mapper;

import com.mysms.students.sis.api.model.SubjectRegistrationLine;
import com.mysms.core.web.Etags;
import com.mysms.students.sis.domain.SubjectRegistrationLineEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for sis.subject_registration_line. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = SisMapperConfig.class, imports = {Etags.class})
public interface SubjectRegistrationLineMapper {

    @Mapping(target = "selectionType", expression =
            "java(entity.getSelectionType() == null ? null : "
                    + "SubjectRegistrationLine.SelectionTypeEnum.fromValue(entity.getSelectionType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SubjectRegistrationLine toDto(SubjectRegistrationLineEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "subjectRegistrationId", ignore = true)
    @Mapping(target = "selectionType", expression =
            "java(dto.getSelectionType() == null ? null : dto.getSelectionType().getValue())")
    SubjectRegistrationLineEntity toEntity(SubjectRegistrationLine dto);
}
