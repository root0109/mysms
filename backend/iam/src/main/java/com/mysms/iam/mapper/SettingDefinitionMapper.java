package com.mysms.iam.mapper;

import com.mysms.iam.api.model.SettingDefinition;
import com.mysms.core.web.Etags;
import com.mysms.iam.domain.SettingDefinitionEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Catalogue row → contract DTO (read-only resource: there is no reverse mapping). */
@Mapper(config = IamMapperConfig.class, imports = Etags.class)
public interface SettingDefinitionMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SettingDefinition toDto(SettingDefinitionEntity entity);

    default SettingDefinition.ValueTypeEnum valueType(String value) {
        return value == null ? null : SettingDefinition.ValueTypeEnum.fromValue(value);
    }

    default List<SettingDefinition.AllowedScopesEnum> allowedScopes(List<String> scopes) {
        return scopes == null ? null : scopes.stream().map(SettingDefinition.AllowedScopesEnum::fromValue).toList();
    }
}
