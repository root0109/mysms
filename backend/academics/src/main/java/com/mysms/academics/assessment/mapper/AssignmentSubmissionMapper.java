package com.mysms.academics.assessment.mapper;

import com.mysms.academics.assessment.domain.AssignmentSubmissionEntity;
import com.mysms.academics.assessment.api.model.AssignmentSubmission;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.assignment_submission to and from its contract DTO. The way in ignores id, tenant, audit and
 * read-only fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface AssignmentSubmissionMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : AssignmentSubmission.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "marksObtained", expression = "java(WireValues.decimal(entity.getMarksObtained()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AssignmentSubmission toDto(AssignmentSubmissionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "marksObtained", ignore = true)
    @Mapping(target = "gradedByUserId", ignore = true)
    @Mapping(target = "gradedOn", ignore = true)
    @Mapping(target = "feedback", ignore = true)
    AssignmentSubmissionEntity toEntity(AssignmentSubmission dto);
}
