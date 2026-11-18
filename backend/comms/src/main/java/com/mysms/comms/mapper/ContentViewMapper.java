package com.mysms.comms.mapper;

import com.mysms.comms.domain.ContentViewEntity;
import com.mysms.comms.api.model.ContentView;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.content_view to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface ContentViewMapper {

    @Mapping(target = "contentType", expression =
            "java(entity.getContentType() == null ? null : "
                    + "ContentView.ContentTypeEnum.fromValue(entity.getContentType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ContentView toDto(ContentViewEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "contentType", expression =
            "java(dto.getContentType() == null ? null : dto.getContentType().getValue())")
    ContentViewEntity toEntity(ContentView dto);
}
