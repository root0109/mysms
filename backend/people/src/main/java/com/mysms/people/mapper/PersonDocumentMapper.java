package com.mysms.people.mapper;

import com.mysms.people.api.model.PersonDocument;
import com.mysms.core.web.Etags;
import com.mysms.people.domain.PersonDocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for people.person_document. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PeopleMapperConfig.class, imports = {Etags.class})
public interface PersonDocumentMapper {

    @Mapping(target = "documentType", expression =
            "java(entity.getDocumentType() == null ? null : "
                    + "PersonDocument.DocumentTypeEnum.fromValue(entity.getDocumentType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : PersonDocument.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PersonDocument toDto(PersonDocumentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "documentType", expression =
            "java(dto.getDocumentType() == null ? null : dto.getDocumentType().getValue())")
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    PersonDocumentEntity toEntity(PersonDocument dto);
}
