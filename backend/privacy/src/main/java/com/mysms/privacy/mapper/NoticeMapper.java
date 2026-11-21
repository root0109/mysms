package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.Notice;
import com.mysms.privacy.domain.NoticeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.notice to and from its contract DTO. The way in ignores id, tenant and the audit set, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface NoticeMapper {

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
    NoticeEntity toEntity(Notice dto);
}
