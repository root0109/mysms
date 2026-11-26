package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.CohortFeedback;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.CohortFeedbackEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.cohort_feedback to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CohortFeedbackMapper {

    @Mapping(target = "contentRating", expression = "java(WireValues.decimal(entity.getContentRating()))")
    @Mapping(target = "teacherRating", expression = "java(WireValues.decimal(entity.getTeacherRating()))")
    @Mapping(target = "valueRating", expression = "java(WireValues.decimal(entity.getValueRating()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CohortFeedback toDto(CohortFeedbackEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "cohortId", ignore = true)
    @Mapping(target = "contentRating", expression = "java(WireValues.decimal(dto.getContentRating()))")
    @Mapping(target = "teacherRating", expression = "java(WireValues.decimal(dto.getTeacherRating()))")
    @Mapping(target = "valueRating", expression = "java(WireValues.decimal(dto.getValueRating()))")
    CohortFeedbackEntity toEntity(CohortFeedback dto);
}
