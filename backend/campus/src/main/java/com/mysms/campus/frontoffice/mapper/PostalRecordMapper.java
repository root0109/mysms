package com.mysms.campus.frontoffice.mapper;

import com.mysms.campus.frontoffice.api.model.PostalRecord;
import com.mysms.core.web.Etags;
import com.mysms.campus.frontoffice.domain.PostalRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps frontoffice.postal_record to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
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
