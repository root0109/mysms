package com.mysms.academics.assessment.mapper;

import com.mysms.academics.assessment.domain.AssignmentTypeEntity;
import com.mysms.academics.assessment.api.model.AssignmentType;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.assignment_type to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface AssignmentTypeMapper {

    @Mapping(target = "gradingBasis", expression =
            "java(entity.getGradingBasis() == null ? null : "
                    + "AssignmentType.GradingBasisEnum.fromValue(entity.getGradingBasis()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AssignmentType toDto(AssignmentTypeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "gradingBasis", expression =
            "java(dto.getGradingBasis() == null ? null : dto.getGradingBasis().getValue())")
    AssignmentTypeEntity toEntity(AssignmentType dto);
}
