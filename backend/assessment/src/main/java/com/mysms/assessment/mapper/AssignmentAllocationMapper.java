package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.AssignmentAllocationEntity;
import com.mysms.assessment.api.model.AssignmentAllocation;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.assignment_allocation to and from its contract DTO. The way in ignores id, tenant, audit and
 * read-only fields, so a request cannot set them; the ETag comes from row_version.
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
