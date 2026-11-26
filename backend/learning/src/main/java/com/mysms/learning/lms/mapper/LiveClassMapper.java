package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.LiveClass;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.LiveClassEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.live_class to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface LiveClassMapper {

    @Mapping(target = "meetingPasswordMasked", ignore = true)
    @Mapping(target = "moderatorPasswordMasked", ignore = true)
    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "conferencingProvider", expression =
            "java(entity.getConferencingProvider() == null ? null : "
                    + "LiveClass.ConferencingProviderEnum.fromValue(entity.getConferencingProvider()))")
    @Mapping(target = "meetingPassword", ignore = true)
    @Mapping(target = "autoRecording", expression =
            "java(entity.getAutoRecording() == null ? null : "
                    + "LiveClass.AutoRecordingEnum.fromValue(entity.getAutoRecording()))")
    @Mapping(target = "inviteStatus", expression =
            "java(entity.getInviteStatus() == null ? null : "
                    + "LiveClass.InviteStatusEnum.fromValue(entity.getInviteStatus()))")
    @Mapping(target = "moderatorPassword", ignore = true)

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    LiveClass toDto(LiveClassEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "conferencingProvider", expression =
            "java(dto.getConferencingProvider() == null ? null : dto.getConferencingProvider().getValue())")
    @Mapping(target = "meetingPasswordEnc", ignore = true)
    @Mapping(target = "autoRecording", expression =
            "java(dto.getAutoRecording() == null ? null : dto.getAutoRecording().getValue())")
    @Mapping(target = "inviteStatus", expression =
            "java(dto.getInviteStatus() == null ? null : dto.getInviteStatus().getValue())")
    @Mapping(target = "moderatorPasswordEnc", ignore = true)
    LiveClassEntity toEntity(LiveClass dto);
}
