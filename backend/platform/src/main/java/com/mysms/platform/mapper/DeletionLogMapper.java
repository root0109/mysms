package com.mysms.platform.mapper;

import com.mysms.platform.api.model.DeletionLog;
import com.mysms.core.web.Etags;
import com.mysms.platform.domain.DeletionLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for platform.deletion_log. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PlatformMapperConfig.class, imports = {Etags.class})
public interface DeletionLogMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    DeletionLog toDto(DeletionLogEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)

    DeletionLogEntity toEntity(DeletionLog dto);
}
