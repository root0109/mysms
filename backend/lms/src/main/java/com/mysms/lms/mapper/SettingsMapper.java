package com.mysms.lms.mapper;

import com.mysms.core.web.Etags;
import com.mysms.lms.api.model.Settings;
import com.mysms.lms.domain.SettingsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** lms.settings → the contract's Settings; the image-search key is never returned (the service adds its mask). */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface SettingsMapper {

    @Mapping(target = "textDirection", expression = "java(entity.getTextDirection() == null ? null : "
            + "Settings.TextDirectionEnum.fromValue(entity.getTextDirection()))")
    @Mapping(target = "notifyPublishedCourses", expression = "java(entity.getNotifyPublishedCourses() == null ? null"
            + " : Settings.NotifyPublishedCoursesEnum.fromValue(entity.getNotifyPublishedCourses()))")
    @Mapping(target = "notifyPublishedCohorts", expression = "java(entity.getNotifyPublishedCohorts() == null ? null"
            + " : Settings.NotifyPublishedCohortsEnum.fromValue(entity.getNotifyPublishedCohorts()))")
    @Mapping(target = "unsplashAccessKey", ignore = true)
    @Mapping(target = "unsplashAccessKeyMasked", ignore = true)
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Settings toDto(SettingsEntity entity);
}
