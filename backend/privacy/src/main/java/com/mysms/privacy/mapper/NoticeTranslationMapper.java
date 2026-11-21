package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.NoticeTranslation;
import com.mysms.privacy.domain.NoticeTranslationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.notice_translation to and from its contract DTO. The way in ignores id, tenant, the audit set, the
 * parent and the content hash, which the service computes on every write.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface NoticeTranslationMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    NoticeTranslation toDto(NoticeTranslationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "noticeVersionId", ignore = true)
    @Mapping(target = "contentSha256", ignore = true)
    NoticeTranslationEntity toEntity(NoticeTranslation dto);
}
