package com.mysms.comms.mapper;

import com.mysms.comms.domain.NotificationChannelPropertyEntity;
import com.mysms.comms.api.model.NotificationChannelProperty;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.notification_channel_property to and from its contract DTO. The way in ignores id, tenant, audit and
 * read-only fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface NotificationChannelPropertyMapper {

    @Mapping(target = "secretValueMasked", ignore = true)
    @Mapping(target = "secretValue", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    NotificationChannelProperty toDto(NotificationChannelPropertyEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "notificationChannelId", ignore = true)
    @Mapping(target = "secretValueEnc", ignore = true)
    NotificationChannelPropertyEntity toEntity(NotificationChannelProperty dto);
}
