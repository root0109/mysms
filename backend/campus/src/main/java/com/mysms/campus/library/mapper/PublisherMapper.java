package com.mysms.campus.library.mapper;

import com.mysms.campus.library.api.model.Publisher;
import com.mysms.core.web.Etags;
import com.mysms.campus.library.domain.PublisherEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps library.publisher to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LibraryMapperConfig.class, imports = {Etags.class})
public interface PublisherMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Publisher toDto(PublisherEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    PublisherEntity toEntity(Publisher dto);
}
