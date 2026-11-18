package com.mysms.library.mapper;

import com.mysms.library.api.model.MediaUnit;
import com.mysms.core.web.Etags;
import com.mysms.library.domain.MediaUnitEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps library.media_unit to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LibraryMapperConfig.class, imports = {Etags.class})
public interface MediaUnitMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : MediaUnit.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    MediaUnit toDto(MediaUnitEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "mediaId", ignore = true)
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    MediaUnitEntity toEntity(MediaUnit dto);
}
