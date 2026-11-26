package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.Annotation;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.AnnotationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.annotation to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface AnnotationMapper {

    @Mapping(target = "annotationType", expression =
            "java(entity.getAnnotationType() == null ? null : "
                    + "Annotation.AnnotationTypeEnum.fromValue(entity.getAnnotationType()))")
    @Mapping(target = "visibility", expression =
            "java(entity.getVisibility() == null ? null : Annotation.VisibilityEnum.fromValue(entity.getVisibility()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Annotation toDto(AnnotationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "annotationType", expression =
            "java(dto.getAnnotationType() == null ? null : dto.getAnnotationType().getValue())")
    @Mapping(target = "visibility", expression =
            "java(dto.getVisibility() == null ? null : dto.getVisibility().getValue())")
    AnnotationEntity toEntity(Annotation dto);
}
