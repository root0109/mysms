package com.mysms.comms.mapper;

import com.mysms.comms.domain.ConversationEntity;
import com.mysms.comms.api.model.Conversation;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.conversation to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface ConversationMapper {

    @Mapping(target = "conversationType", expression =
            "java(entity.getConversationType() == null ? null : "
                    + "Conversation.ConversationTypeEnum.fromValue(entity.getConversationType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Conversation toDto(ConversationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "conversationType", expression =
            "java(dto.getConversationType() == null ? null : dto.getConversationType().getValue())")
    ConversationEntity toEntity(Conversation dto);
}
