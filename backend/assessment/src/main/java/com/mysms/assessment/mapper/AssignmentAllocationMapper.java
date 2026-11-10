package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.AssignmentAllocationEntity;
import com.mysms.assessment.api.model.AssignmentAllocation;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for assessment.assignment_allocation. The way in ignores the id, the tenant, the audit set and
 * every read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface AssignmentAllocationMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AssignmentAllocation toDto(AssignmentAllocationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "assignmentId", ignore = true)
    AssignmentAllocationEntity toEntity(AssignmentAllocation dto);
}
