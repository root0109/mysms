package com.mysms.iam.mapper;

import com.mysms.core.web.Etags;
import com.mysms.iam.api.model.AuthToken;
import com.mysms.iam.domain.AuthTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity → contract DTO for personal API tokens. There is no way in: tokens are created by iam.CreateApiToken from its
 * own request, and the secret or its hash is never part of the resource. The ETag comes from row_version.
 */
@Mapper(config = IamMapperConfig.class, imports = {Etags.class})
public interface AuthTokenMapper {

    @Mapping(target = "tokenType", expression = "java(AuthToken.TokenTypeEnum.fromValue(entity.getTokenType()))")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    AuthToken toDto(AuthTokenEntity entity);
}
