package com.mysms.lms.mapper;

import com.mysms.core.web.Etags;
import com.mysms.lms.api.model.Lesson;
import com.mysms.lms.domain.LessonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.lesson. The way in ignores the id, the tenant, the audit set and the version fields
 * (version, superseded_by_id, published_at: only workflows change them). The two editor documents are JSON values
 * the service converts with the one JSON mapper (LessonService), so they are left out here on both ways.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface LessonMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    @Mapping(target = "fileType", expression =
            "java(entity.getFileType() == null ? null : Lesson.FileTypeEnum.fromValue(entity.getFileType()))")
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "teacherContent", ignore = true)
    Lesson toDto(LessonEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "supersededById", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "teacherContent", ignore = true)
    @Mapping(target = "fileType", expression = "java(dto.getFileType() == null ? null : dto.getFileType().getValue())")
    LessonEntity toEntity(Lesson dto);
}
