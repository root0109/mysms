package com.mysms.comms.mapper;

import com.mysms.comms.domain.MessageTemplateEntity;
import com.mysms.comms.api.model.MessageTemplate;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.message_template to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface MessageTemplateMapper {

    @Mapping(target = "channel", expression =
            "java(entity.getChannel() == null ? null : MessageTemplate.ChannelEnum.fromValue(entity.getChannel()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    MessageTemplate toDto(MessageTemplateEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "channel", expression = "java(dto.getChannel() == null ? null : dto.getChannel().getValue())")
    MessageTemplateEntity toEntity(MessageTemplate dto);
}
