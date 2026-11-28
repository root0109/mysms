package com.mysms.academics.assessment.mapper;

import com.mysms.academics.assessment.domain.AssignmentApprovalEntity;
import com.mysms.academics.assessment.api.model.AssignmentApproval;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.assignment_approval to its contract DTO; there is no way in, since only the assignment's workflow
 * writes approvals. The ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface AssignmentApprovalMapper {

    @Mapping(target = "decision", expression =
            "java(entity.getDecision() == null ? null : "
                    + "AssignmentApproval.DecisionEnum.fromValue(entity.getDecision()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AssignmentApproval toDto(AssignmentApprovalEntity entity);
}
