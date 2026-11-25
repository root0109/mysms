package com.mysms.comms.mapper;

import com.mysms.comms.api.model.InAppNotification;
import com.mysms.comms.repository.InAppNotificationRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps a comms.in_app_notification row to its contract DTO. Notifications are never written through the API, and the
 * table has no row version, so there is no way in and no ETag.
 */
@Mapper(config = CommsMapperConfig.class)
public interface InAppNotificationMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "etag", ignore = true)
    @Mapping(target = "emailStatus", expression = "java(row.emailStatus() == null ? null : "
            + "InAppNotification.EmailStatusEnum.fromValue(row.emailStatus()))")
    @Mapping(target = "smsStatus", expression = "java(row.smsStatus() == null ? null : "
            + "InAppNotification.SmsStatusEnum.fromValue(row.smsStatus()))")
    @Mapping(target = "popupStatus", expression = "java(row.popupStatus() == null ? null : "
            + "InAppNotification.PopupStatusEnum.fromValue(row.popupStatus()))")
    @Mapping(target = "digestStatus", expression = "java(row.digestStatus() == null ? null : "
            + "InAppNotification.DigestStatusEnum.fromValue(row.digestStatus()))")
    InAppNotification toDto(InAppNotificationRow row);
}
