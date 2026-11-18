package com.mysms.lms.mapper;

import com.mysms.core.web.Etags;
import com.mysms.lms.api.model.LessonNote;
import com.mysms.lms.domain.LessonNoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.lesson_note. The way in ignores the id, the tenant, the audit set, the lesson (the
 * path's) and the writer (always the caller, set by the service).
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface LessonNoteMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    @Mapping(target = "color", expression =
            "java(entity.getColor() == null ? null : LessonNote.ColorEnum.fromValue(entity.getColor()))")
    LessonNote toDto(LessonNoteEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "color", expression = "java(dto.getColor() == null ? null : dto.getColor().getValue())")
    LessonNoteEntity toEntity(LessonNote dto);
}
