package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.LmsAssignmentSubmission;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.LmsAssignmentSubmissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.assignment_submission to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface LmsAssignmentSubmissionMapper {

    @Mapping(target = "submissionType", expression =
            "java(entity.getSubmissionType() == null ? null : "
                    + "LmsAssignmentSubmission.SubmissionTypeEnum.fromValue(entity.getSubmissionType()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : "
                    + "LmsAssignmentSubmission.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LmsAssignmentSubmission toDto(LmsAssignmentSubmissionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "submissionType", expression =
            "java(dto.getSubmissionType() == null ? null : dto.getSubmissionType().getValue())")
    @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getValue())")
    LmsAssignmentSubmissionEntity toEntity(LmsAssignmentSubmission dto);
}
