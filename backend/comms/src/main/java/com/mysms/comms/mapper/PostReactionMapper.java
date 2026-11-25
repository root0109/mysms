package com.mysms.comms.mapper;

import com.mysms.comms.api.model.PostReaction;
import com.mysms.comms.domain.ReactionEntity;
import com.mysms.core.web.Etags;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps a comms.reaction row on a post to and from the PostReaction DTO. The way in ignores id, tenant, audit, the
 * post (from the path) and the user (from the token); the ETag comes from row_version.
 */
@Mapper(config = CommsMapperConfig.class, imports = {Etags.class})
public interface PostReactionMapper {

    @Mapping(target = "reaction", expression =
            "java(entity.getReaction() == null ? null : PostReaction.ReactionEnum.fromValue(entity.getReaction()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    PostReaction toDto(ReactionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "postId", ignore = true)
    @Mapping(target = "commentId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "reaction", expression = "java(dto.getReaction() == null ? null : dto.getReaction().getValue())")
    ReactionEntity toEntity(PostReaction dto);
}
