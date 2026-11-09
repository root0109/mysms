package com.mysms.iam.mapper;

import com.mysms.contracts.iam.model.Permission;
import com.mysms.core.web.Etags;
import com.mysms.iam.domain.PermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Catalogue row → contract DTO (read-only resource: there is no way in). */
@Mapper(config = IamMapperConfig.class, imports = Etags.class)
public interface PermissionMapper {

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Permission toDto(PermissionEntity entity);
}
