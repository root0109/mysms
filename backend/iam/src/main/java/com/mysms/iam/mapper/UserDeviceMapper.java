package com.mysms.iam.mapper;

import com.mysms.iam.api.model.UserDevice;
import com.mysms.core.web.Etags;
import com.mysms.iam.domain.UserDeviceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for iam.user_device. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = IamMapperConfig.class, imports = {Etags.class})
public interface UserDeviceMapper {

    @Mapping(target = "platform", expression =
            "java(entity.getPlatform() == null ? null : UserDevice.PlatformEnum.fromValue(entity.getPlatform()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    UserDevice toDto(UserDeviceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "platform", expression = "java(dto.getPlatform() == null ? null : dto.getPlatform().getValue())")
    UserDeviceEntity toEntity(UserDevice dto);
}
