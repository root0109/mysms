package com.mysms.identity.iam.mapper;

import com.mysms.identity.iam.api.model.UserDevice;
import com.mysms.core.web.Etags;
import com.mysms.identity.iam.domain.UserDeviceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps iam.user_device to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
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
