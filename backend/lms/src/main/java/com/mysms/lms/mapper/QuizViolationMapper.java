package com.mysms.lms.mapper;

import com.mysms.lms.api.model.QuizViolation;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.QuizViolationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.quiz_violation to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface QuizViolationMapper {

    @Mapping(target = "eventType", expression =
            "java(entity.getEventType() == null ? null : QuizViolation.EventTypeEnum.fromValue(entity.getEventType()))")
    @Mapping(target = "severity", expression =
            "java(entity.getSeverity() == null ? null : QuizViolation.SeverityEnum.fromValue(entity.getSeverity()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    QuizViolation toDto(QuizViolationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "quizSubmissionId", ignore = true)
    @Mapping(target = "eventType", expression =
            "java(dto.getEventType() == null ? null : dto.getEventType().getValue())")
    @Mapping(target = "severity", expression = "java(dto.getSeverity() == null ? null : dto.getSeverity().getValue())")
    QuizViolationEntity toEntity(QuizViolation dto);
}
