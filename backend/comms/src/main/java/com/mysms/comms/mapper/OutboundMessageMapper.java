package com.mysms.comms.mapper;

import com.mysms.comms.domain.OutboundMessageEntity;
import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.outbound_message to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface OutboundMessageMapper {

    @Mapping(target = "messageKind", expression =
            "java(entity.getMessageKind() == null ? null : "
                    + "OutboundMessage.MessageKindEnum.fromValue(entity.getMessageKind()))")
    @Mapping(target = "channel", expression =
            "java(entity.getChannel() == null ? null : OutboundMessage.ChannelEnum.fromValue(entity.getChannel()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : OutboundMessage.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    OutboundMessage toDto(OutboundMessageEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "templateParams", ignore = true)
    @Mapping(target = "actionUrl", ignore = true)
    @Mapping(target = "messageKind", expression =
            "java(dto.getMessageKind() == null ? null : dto.getMessageKind().getValue())")
    @Mapping(target = "channel", expression = "java(dto.getChannel() == null ? null : dto.getChannel().getValue())")
    @Mapping(target = "status", ignore = true)
    OutboundMessageEntity toEntity(OutboundMessage dto);
}
