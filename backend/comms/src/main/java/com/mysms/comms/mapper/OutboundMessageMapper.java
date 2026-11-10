package com.mysms.comms.mapper;

import com.mysms.comms.domain.OutboundMessageEntity;
import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for comms.outbound_message. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
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
