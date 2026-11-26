package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.ReleaseRuleCondition;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.ReleaseRuleConditionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.release_rule_condition to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ReleaseRuleConditionMapper {

    @Mapping(target = "conditionType", expression =
            "java(entity.getConditionType() == null ? null : "
                    + "ReleaseRuleCondition.ConditionTypeEnum.fromValue(entity.getConditionType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ReleaseRuleCondition toDto(ReleaseRuleConditionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "conditionType", expression =
            "java(dto.getConditionType() == null ? null : dto.getConditionType().getValue())")
    @Mapping(target = "releaseRuleId", ignore = true)
    ReleaseRuleConditionEntity toEntity(ReleaseRuleCondition dto);
}
