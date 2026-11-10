package com.mysms.iam.mapper;

import com.mysms.iam.api.model.RolePermission;
import com.mysms.core.web.Etags;
import com.mysms.iam.domain.RolePermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for iam.role_permission. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = IamMapperConfig.class, imports = {Etags.class})
public interface RolePermissionMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    RolePermission toDto(RolePermissionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "roleId", ignore = true)
    RolePermissionEntity toEntity(RolePermission dto);
}
