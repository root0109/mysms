package com.mysms.learning.lms.mapper;

import com.mysms.learning.lms.api.model.DriveItemShare;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.domain.DriveItemShareEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps lms.drive_item_share to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class})
public interface DriveItemShareMapper {

    @Mapping(target = "reaction", expression =
            "java(entity.getReaction() == null ? null : DriveItemShare.ReactionEnum.fromValue(entity.getReaction()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    DriveItemShare toDto(DriveItemShareEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "driveItemId", ignore = true)
    @Mapping(target = "reaction", expression = "java(dto.getReaction() == null ? null : dto.getReaction().getValue())")
    DriveItemShareEntity toEntity(DriveItemShare dto);
}
