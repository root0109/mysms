package com.mysms.identity.iam.mapper;

import com.mysms.core.web.Etags;
import com.mysms.identity.iam.api.model.ApiPermission;
import com.mysms.identity.iam.domain.ApiPermissionEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Catalogue row → contract DTO (read-only resource: there is no way in; rows come from published catalogues). */
@Mapper(config = IamMapperConfig.class, imports = {Etags.class, List.class})
public interface ApiPermissionMapper {

    @Mapping(target = "schemes",
            expression = "java(entity.getSchemes().isBlank() ? List.of() : List.of(entity.getSchemes().split(\" \")))")
    @Mapping(target = "firstSeenTime", source = "firstSeenAt")
    @Mapping(target = "lastSeenTime", source = "lastSeenAt")
    @Mapping(target = "retireTime", source = "retiredAt")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    ApiPermission toDto(ApiPermissionEntity entity);
}
