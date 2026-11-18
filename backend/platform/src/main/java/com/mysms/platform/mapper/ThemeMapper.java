package com.mysms.platform.mapper;

import com.mysms.core.web.Etags;
import com.mysms.platform.api.model.Theme;
import com.mysms.platform.domain.ThemeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for platform.theme. The way in ignores the id, the tenant and the audit set; the ETag always
 * comes from row_version. The design tokens are rows of platform.theme_token: the service reads and writes them.
 */
@Mapper(config = PlatformMapperConfig.class, imports = Etags.class)
public interface ThemeMapper {

    @Mapping(target = "tokens", ignore = true)
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Theme toDto(ThemeEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    ThemeEntity toEntity(Theme dto);
}
