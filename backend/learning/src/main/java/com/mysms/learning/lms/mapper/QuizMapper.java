package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.Quiz;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.QuizEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.quiz to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface QuizMapper {

    @Mapping(target = "totalMarks", expression = "java(WireValues.decimal(entity.getTotalMarks()))")
    @Mapping(target = "negativeMarks", expression = "java(WireValues.decimal(entity.getNegativeMarks()))")
    @Mapping(target = "marksPerQuestion", expression = "java(WireValues.decimal(entity.getMarksPerQuestion()))")
    @Mapping(target = "attemptScoring", expression =
            "java(entity.getAttemptScoring() == null ? null : "
                    + "Quiz.AttemptScoringEnum.fromValue(entity.getAttemptScoring()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Quiz toDto(QuizEntity entity);

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
    @Mapping(target = "totalMarks", expression = "java(WireValues.decimal(dto.getTotalMarks()))")
    @Mapping(target = "negativeMarks", expression = "java(WireValues.decimal(dto.getNegativeMarks()))")
    @Mapping(target = "marksPerQuestion", expression = "java(WireValues.decimal(dto.getMarksPerQuestion()))")
    @Mapping(target = "attemptScoring", expression =
            "java(dto.getAttemptScoring() == null ? null : dto.getAttemptScoring().getValue())")
    QuizEntity toEntity(Quiz dto);
}
