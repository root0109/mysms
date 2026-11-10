package com.mysms.comms.mapper;

import com.mysms.comms.domain.MailboxMessageEntity;
import com.mysms.comms.api.model.MailboxMessage;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for comms.mailbox_message. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface MailboxMessageMapper {

    @Mapping(target = "folder", expression =
            "java(entity.getFolder() == null ? null : MailboxMessage.FolderEnum.fromValue(entity.getFolder()))")
    @Mapping(target = "deliveryStatus", expression =
            "java(entity.getDeliveryStatus() == null ? null : "
                    + "MailboxMessage.DeliveryStatusEnum.fromValue(entity.getDeliveryStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    MailboxMessage toDto(MailboxMessageEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "folder", expression = "java(dto.getFolder() == null ? null : dto.getFolder().getValue())")
    @Mapping(target = "deliveryStatus", expression =
            "java(dto.getDeliveryStatus() == null ? null : dto.getDeliveryStatus().getValue())")
    MailboxMessageEntity toEntity(MailboxMessage dto);
}
