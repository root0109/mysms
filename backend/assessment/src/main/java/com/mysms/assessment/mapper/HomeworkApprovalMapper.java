package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.HomeworkApprovalEntity;
import com.mysms.assessment.api.model.HomeworkApproval;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.homework_approval to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface HomeworkApprovalMapper {

    @Mapping(target = "decision", expression =
            "java(entity.getDecision() == null ? null : HomeworkApproval.DecisionEnum.fromValue(entity.getDecision()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    HomeworkApproval toDto(HomeworkApprovalEntity entity);
}
