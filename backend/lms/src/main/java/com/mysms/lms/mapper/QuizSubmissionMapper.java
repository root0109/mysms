package com.mysms.lms.mapper;

import com.mysms.lms.api.model.QuizSubmission;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.QuizSubmissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.quiz_submission to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface QuizSubmissionMapper {

    @Mapping(target = "score", expression = "java(WireValues.decimal(entity.getScore()))")
    @Mapping(target = "scoreOutOf", expression = "java(WireValues.decimal(entity.getScoreOutOf()))")
    @Mapping(target = "percentage", expression = "java(WireValues.decimal(entity.getPercentage()))")
    @Mapping(target = "passingPercentage", expression = "java(WireValues.decimal(entity.getPassingPercentage()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : QuizSubmission.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    QuizSubmission toDto(QuizSubmissionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "attemptNumber", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "scoreOutOf", ignore = true)
    @Mapping(target = "percentage", ignore = true)
    @Mapping(target = "passingPercentage", expression = "java(WireValues.decimal(dto.getPassingPercentage()))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    QuizSubmissionEntity toEntity(QuizSubmission dto);
}
