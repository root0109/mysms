package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.AssignmentSubmissionEntity;
import com.mysms.assessment.api.model.AssignmentSubmission;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for assessment.assignment_submission. The way in ignores the id, the tenant, the audit set and
 * every read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
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
    @Mapping(target = "marksObtained", expression = "java(WireValues.decimal(dto.getMarksObtained()))")
    AssignmentSubmissionEntity toEntity(AssignmentSubmission dto);
}
