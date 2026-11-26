package com.mysms.identity.iam.mapper;

import com.mysms.identity.iam.api.model.UserSession;
import com.mysms.core.web.Etags;
import com.mysms.identity.iam.domain.UserSessionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps iam.user_session to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = IamMapperConfig.class, imports = {Etags.class})
public interface UserSessionMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    UserSession toDto(UserSessionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "userId", ignore = true)
    UserSessionEntity toEntity(UserSession dto);
}
