package com.mysms.platform.mapper;

import com.mysms.core.web.Etags;
import com.mysms.platform.api.model.Tag;
import com.mysms.platform.domain.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for platform.tag. The way in ignores the id, the tenant and the audit set; the ETag always comes
 * from row_version. {@code translations} has no column: it is never returned, and the service refuses it on the way in.
 */
@Mapper(config = PlatformMapperConfig.class, imports = Etags.class)
public interface TagMapper {

    @Mapping(target = "translations", ignore = true)
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Tag toDto(TagEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    TagEntity toEntity(Tag dto);
}
