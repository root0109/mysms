package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.QuizAnswer;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.QuizAnswerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.quiz_answer to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface QuizAnswerMapper {

    @Mapping(target = "marksAwarded", expression = "java(WireValues.decimal(entity.getMarksAwarded()))")
    @Mapping(target = "marksPossible", expression = "java(WireValues.decimal(entity.getMarksPossible()))")
    @Mapping(target = "timeSpentMs", expression = "java(WireValues.int64(entity.getTimeSpentMs()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    QuizAnswer toDto(QuizAnswerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "quizSubmissionId", ignore = true)
    @Mapping(target = "marksAwarded", expression = "java(WireValues.decimal(dto.getMarksAwarded()))")
    @Mapping(target = "marksPossible", expression = "java(WireValues.decimal(dto.getMarksPossible()))")
    @Mapping(target = "timeSpentMs", expression = "java(WireValues.int64(dto.getTimeSpentMs()))")
    QuizAnswerEntity toEntity(QuizAnswer dto);
}
