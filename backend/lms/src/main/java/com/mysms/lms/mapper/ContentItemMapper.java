package com.mysms.lms.mapper;

import com.mysms.lms.api.model.ContentItem;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.ContentItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.content_item. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface ContentItemMapper {

    @Mapping(target = "itemKind", expression =
            "java(entity.getItemKind() == null ? null : ContentItem.ItemKindEnum.fromValue(entity.getItemKind()))")
    @Mapping(target = "visibleStartTime", expression = "java(WireValues.time(entity.getVisibleStartTime()))")
    @Mapping(target = "visibleEndTime", expression = "java(WireValues.time(entity.getVisibleEndTime()))")
    @Mapping(target = "attemptStartTime", expression = "java(WireValues.time(entity.getAttemptStartTime()))")
    @Mapping(target = "attemptEndTime", expression = "java(WireValues.time(entity.getAttemptEndTime()))")
    @Mapping(target = "points", expression = "java(WireValues.decimal(entity.getPoints()))")
    @Mapping(target = "weightPercent", expression = "java(WireValues.decimal(entity.getWeightPercent()))")
    @Mapping(target = "viewCount", expression = "java(WireValues.int64(entity.getViewCount()))")
    @Mapping(target = "likeCount", expression = "java(WireValues.int64(entity.getLikeCount()))")
    @Mapping(target = "ratingTotal", expression = "java(WireValues.decimal(entity.getRatingTotal()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ContentItem toDto(ContentItemEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "itemKind", expression = "java(dto.getItemKind() == null ? null : dto.getItemKind().getValue())")
    @Mapping(target = "visibleStartTime", expression = "java(WireValues.time(dto.getVisibleStartTime()))")
    @Mapping(target = "visibleEndTime", expression = "java(WireValues.time(dto.getVisibleEndTime()))")
    @Mapping(target = "attemptStartTime", expression = "java(WireValues.time(dto.getAttemptStartTime()))")
    @Mapping(target = "attemptEndTime", expression = "java(WireValues.time(dto.getAttemptEndTime()))")
    @Mapping(target = "points", expression = "java(WireValues.decimal(dto.getPoints()))")
    @Mapping(target = "weightPercent", expression = "java(WireValues.decimal(dto.getWeightPercent()))")
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "ratingTotal", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    ContentItemEntity toEntity(ContentItem dto);
}
