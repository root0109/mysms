package com.mysms.lms.mapper;

import com.mysms.lms.api.model.Badge;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.BadgeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.badge to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface BadgeMapper {

    @Mapping(target = "triggerEvent", expression =
            "java(entity.getTriggerEvent() == null ? null : "
                    + "Badge.TriggerEventEnum.fromValue(entity.getTriggerEvent()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Badge toDto(BadgeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "triggerEvent", expression =
            "java(dto.getTriggerEvent() == null ? null : dto.getTriggerEvent().getValue())")
    BadgeEntity toEntity(Badge dto);
}
