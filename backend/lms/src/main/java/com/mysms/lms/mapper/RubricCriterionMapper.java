package com.mysms.lms.mapper;

import com.mysms.lms.api.model.RubricCriterion;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.RubricCriterionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.rubric_criterion. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface RubricCriterionMapper {

    @Mapping(target = "weightPercent", expression = "java(WireValues.decimal(entity.getWeightPercent()))")
    @Mapping(target = "groupWeightPercent", expression = "java(WireValues.decimal(entity.getGroupWeightPercent()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    RubricCriterion toDto(RubricCriterionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rubricId", ignore = true)
    @Mapping(target = "weightPercent", expression = "java(WireValues.decimal(dto.getWeightPercent()))")
    @Mapping(target = "groupWeightPercent", expression = "java(WireValues.decimal(dto.getGroupWeightPercent()))")
    RubricCriterionEntity toEntity(RubricCriterion dto);
}
