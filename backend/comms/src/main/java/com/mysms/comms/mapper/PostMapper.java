package com.mysms.comms.mapper;

import com.mysms.comms.domain.PostEntity;
import com.mysms.comms.api.model.Post;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps comms.post to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a request
 * cannot set them; the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface PostMapper {

    @Mapping(target = "visibility", expression =
            "java(entity.getVisibility() == null ? null : Post.VisibilityEnum.fromValue(entity.getVisibility()))")
    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Post.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Post toDto(PostEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "visibility", expression =
            "java(dto.getVisibility() == null ? null : dto.getVisibility().getValue())")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "postedAt", ignore = true)
    PostEntity toEntity(Post dto);
}
