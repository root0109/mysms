package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ExternalToolSetting;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ExternalToolSettingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.external_tool_setting to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface ExternalToolSettingMapper {

    @Mapping(target = "secretValueMasked", ignore = true)
    @Mapping(target = "secretValue", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ExternalToolSetting toDto(ExternalToolSettingEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "externalToolId", ignore = true)
    @Mapping(target = "secretValueEnc", ignore = true)
    ExternalToolSettingEntity toEntity(ExternalToolSetting dto);
}
