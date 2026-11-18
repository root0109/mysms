package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.HomeworkSubmissionEntity;
import com.mysms.assessment.api.model.HomeworkSubmission;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.homework_submission to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface HomeworkSubmissionMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : HomeworkSubmission.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    HomeworkSubmission toDto(HomeworkSubmissionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "checkedByUserId", ignore = true)
    @Mapping(target = "checkedOn", ignore = true)
    @Mapping(target = "teacherComments", ignore = true)
    @Mapping(target = "studentReply", ignore = true)
    HomeworkSubmissionEntity toEntity(HomeworkSubmission dto);
}
