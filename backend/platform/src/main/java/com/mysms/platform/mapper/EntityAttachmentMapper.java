package com.mysms.platform.mapper;

import com.mysms.platform.api.model.EntityAttachment;
import com.mysms.core.web.Etags;
import com.mysms.platform.domain.EntityAttachmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for platform.entity_attachment. The way in ignores the id, tenant, audit set and read-only
 * fields, so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PlatformMapperConfig.class, imports = {Etags.class})
public interface EntityAttachmentMapper {

    @Mapping(target = "purpose", expression =
            "java(entity.getPurpose() == null ? null : EntityAttachment.PurposeEnum.fromValue(entity.getPurpose()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    EntityAttachment toDto(EntityAttachmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "purpose", expression = "java(dto.getPurpose() == null ? null : dto.getPurpose().getValue())")
    EntityAttachmentEntity toEntity(EntityAttachment dto);
}
