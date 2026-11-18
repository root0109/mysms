package com.mysms.library.mapper;

import com.mysms.library.api.model.MediaCourse;
import com.mysms.core.web.Etags;
import com.mysms.library.domain.MediaCourseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps library.media_course to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LibraryMapperConfig.class, imports = {Etags.class})
public interface MediaCourseMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    MediaCourse toDto(MediaCourseEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "mediaId", ignore = true)
    MediaCourseEntity toEntity(MediaCourse dto);
}
