package com.mysms.lms.mapper;

import com.mysms.lms.api.model.SettingsSidebarItem;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.SettingsSidebarItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.settings_sidebar_item to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface SettingsSidebarItemMapper {

    @Mapping(target = "itemType", expression =
            "java(entity.getItemType() == null ? null : "
                    + "SettingsSidebarItem.ItemTypeEnum.fromValue(entity.getItemType()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SettingsSidebarItem toDto(SettingsSidebarItemEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "settingsId", ignore = true)
    @Mapping(target = "itemType", expression = "java(dto.getItemType() == null ? null : dto.getItemType().getValue())")
    SettingsSidebarItemEntity toEntity(SettingsSidebarItem dto);
}
