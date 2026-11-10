package com.mysms.iam.mapper;

import com.mysms.iam.api.model.SettingValue;
import com.mysms.core.web.Etags;
import com.mysms.iam.domain.SettingValueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for iam.setting_value. Output-only fields (id, audit stamps, etag) are never read from a
 * request, and the tenant always comes from the call context.
 */
@Mapper(config = IamMapperConfig.class, imports = Etags.class)
public interface SettingValueMapper {

    @Mapping(target = "isLocked", source = "locked")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SettingValue toDto(SettingValueEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "locked", source = "isLocked", defaultValue = "false")
    SettingValueEntity toEntity(SettingValue dto);

    default SettingValue.ScopeTypeEnum scopeType(String value) {
        return value == null ? null : SettingValue.ScopeTypeEnum.fromValue(value);
    }

    default String scopeType(SettingValue.ScopeTypeEnum value) {
        return value == null ? null : value.getValue();
    }
}
