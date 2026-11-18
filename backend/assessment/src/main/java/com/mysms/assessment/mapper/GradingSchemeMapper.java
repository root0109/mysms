package com.mysms.assessment.mapper;

import com.mysms.assessment.api.model.GradingScheme;
import com.mysms.assessment.domain.GradingSchemeEntity;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity ⇄ contract mapper for grading schemes; JSON criteria stay structured at the API boundary. */
@Mapper(config = AssessmentMapperConfig.class, imports = {Etags.class})
public interface GradingSchemeMapper {

    @Mapping(target = "schemeType", expression = "java(GradingScheme.SchemeTypeEnum.fromValue(entity.getSchemeType()))")
    @Mapping(target = "gradingMethod", expression = "java(entity.getGradingMethod() == null ? null"
            + " : GradingScheme.GradingMethodEnum.fromValue(entity.getGradingMethod()))")
    @Mapping(target = "criteria", expression = "java(GradingCriteriaJson.read(entity.getCriteria()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    GradingScheme toDto(GradingSchemeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "schemeType", expression = "java(dto.getSchemeType() == null ? null : dto.getSchemeType().getValue())")
    @Mapping(target = "gradingMethod",
            expression = "java(dto.getGradingMethod() == null ? null : dto.getGradingMethod().getValue())")
    @Mapping(target = "criteria", expression = "java(GradingCriteriaJson.write(dto.getCriteria()))")
    GradingSchemeEntity toEntity(GradingScheme dto);
}
