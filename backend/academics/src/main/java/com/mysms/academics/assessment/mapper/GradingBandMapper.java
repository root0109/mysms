package com.mysms.academics.assessment.mapper;

import com.mysms.academics.assessment.domain.GradingBandEntity;
import com.mysms.academics.assessment.api.model.GradingBand;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps assessment.grading_band to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface GradingBandMapper {

    @Mapping(target = "minPercent", expression = "java(WireValues.decimal(entity.getMinPercent()))")
    @Mapping(target = "maxPercent", expression = "java(WireValues.decimal(entity.getMaxPercent()))")
    @Mapping(target = "gradePoint", expression = "java(WireValues.decimal(entity.getGradePoint()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    GradingBand toDto(GradingBandEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "minPercent", expression = "java(WireValues.decimal(dto.getMinPercent()))")
    @Mapping(target = "maxPercent", expression = "java(WireValues.decimal(dto.getMaxPercent()))")
    @Mapping(target = "gradePoint", expression = "java(WireValues.decimal(dto.getGradePoint()))")
    GradingBandEntity toEntity(GradingBand dto);
}
