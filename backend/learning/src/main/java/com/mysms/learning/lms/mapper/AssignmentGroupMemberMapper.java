package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.AssignmentGroupMember;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.AssignmentGroupMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.assignment_group_member to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface AssignmentGroupMemberMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : AssignmentGroupMember.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AssignmentGroupMember toDto(AssignmentGroupMemberEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    AssignmentGroupMemberEntity toEntity(AssignmentGroupMember dto);
}
