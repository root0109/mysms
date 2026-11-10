package com.mysms.frontoffice.mapper;

import com.mysms.frontoffice.api.model.PostalRecord;
import com.mysms.core.web.Etags;
import com.mysms.frontoffice.domain.PostalRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for frontoffice.postal_record. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = FrontofficeMapperConfig.class, imports = {Etags.class})
public interface PostalRecordMapper {

    @Mapping(target = "direction", expression =
            "java(entity.getDirection() == null ? null : PostalRecord.DirectionEnum.fromValue(entity.getDirection()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PostalRecord toDto(PostalRecordEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "direction", expression =
            "java(dto.getDirection() == null ? null : dto.getDirection().getValue())")
    PostalRecordEntity toEntity(PostalRecord dto);
}
