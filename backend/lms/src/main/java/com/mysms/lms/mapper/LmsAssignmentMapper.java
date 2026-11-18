package com.mysms.lms.mapper;

import com.mysms.lms.api.model.LmsAssignment;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.LmsAssignmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.assignment to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LmsAssignmentMapper {

    @Mapping(target = "submissionType", expression =
            "java(entity.getSubmissionType() == null ? null : "
                    + "LmsAssignment.SubmissionTypeEnum.fromValue(entity.getSubmissionType()))")
    @Mapping(target = "submissionMode", expression =
            "java(entity.getSubmissionMode() == null ? null : "
                    + "LmsAssignment.SubmissionModeEnum.fromValue(entity.getSubmissionMode()))")
    @Mapping(target = "peerReviewPoints", expression = "java(WireValues.decimal(entity.getPeerReviewPoints()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LmsAssignment toDto(LmsAssignmentEntity entity);

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
    @Mapping(target = "submissionType", expression =
            "java(dto.getSubmissionType() == null ? null : dto.getSubmissionType().getValue())")
    @Mapping(target = "submissionMode", expression =
            "java(dto.getSubmissionMode() == null ? null : dto.getSubmissionMode().getValue())")
    @Mapping(target = "peerReviewPoints", expression = "java(WireValues.decimal(dto.getPeerReviewPoints()))")
    LmsAssignmentEntity toEntity(LmsAssignment dto);
}
