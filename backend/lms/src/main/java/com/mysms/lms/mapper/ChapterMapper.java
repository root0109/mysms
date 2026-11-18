package com.mysms.lms.mapper;

import com.mysms.lms.api.model.Chapter;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ChapterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.chapter to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ChapterMapper {

    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "dueTime", expression = "java(WireValues.time(entity.getDueTime()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Chapter toDto(ChapterEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "dueTime", expression = "java(WireValues.time(dto.getDueTime()))")
    ChapterEntity toEntity(Chapter dto);
}
