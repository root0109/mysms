package com.mysms.comms.mapper;

import com.mysms.comms.domain.NoticeBackgroundEntity;
import com.mysms.comms.api.model.NoticeBackground;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.notice_background to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface NoticeBackgroundMapper {

    @Mapping(target = "usage", expression =
            "java(entity.getUsage() == null ? null : NoticeBackground.UsageEnum.fromValue(entity.getUsage()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    NoticeBackground toDto(NoticeBackgroundEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "usage", expression = "java(dto.getUsage() == null ? null : dto.getUsage().getValue())")
    NoticeBackgroundEntity toEntity(NoticeBackground dto);
}
