package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.AssignmentApprovalEntity;
import com.mysms.assessment.api.model.AssignmentApproval;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.assignment_approval to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "assignmentId", ignore = true)
    @Mapping(target = "decision", expression = "java(dto.getDecision() == null ? null : dto.getDecision().getValue())")
    AssignmentApprovalEntity toEntity(AssignmentApproval dto);
}
