package com.mysms.assessment.mapper;

import com.mysms.assessment.domain.AssignmentEntity;
import com.mysms.assessment.api.model.Assignment;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.assignment to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface AssignmentMapper {

    @Mapping(target = "maxMarks", expression = "java(WireValues.decimal(entity.getMaxMarks()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Assignment.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Assignment toDto(AssignmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "maxMarks", expression = "java(WireValues.decimal(dto.getMaxMarks()))")
    @Mapping(target = "status", ignore = true)
    AssignmentEntity toEntity(Assignment dto);
}
