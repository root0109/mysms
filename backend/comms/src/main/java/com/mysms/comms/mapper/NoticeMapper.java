package com.mysms.comms.mapper;

import com.mysms.comms.domain.NoticeEntity;
import com.mysms.comms.api.model.Notice;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.notice to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface NoticeMapper {

    @Mapping(target = "audience", expression =
            "java(entity.getAudience() == null ? null : Notice.AudienceEnum.fromValue(entity.getAudience()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Notice toDto(NoticeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "audience", expression = "java(dto.getAudience() == null ? null : dto.getAudience().getValue())")
    @Mapping(target = "isPublished", ignore = true)
    NoticeEntity toEntity(Notice dto);
}
