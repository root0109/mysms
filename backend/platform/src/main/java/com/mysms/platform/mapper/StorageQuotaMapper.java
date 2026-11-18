package com.mysms.platform.mapper;

import com.mysms.platform.api.model.StorageQuota;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.platform.domain.StorageQuotaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for platform.storage_quota. The way in ignores the id, tenant, audit set and read-only fields,
 * so a request can never choose an id, re-tenant a row or set a workflow-only value.
 */
@Mapper(config = PlatformMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface StorageQuotaMapper {

    @Mapping(target = "quotaBytes", expression = "java(WireValues.int64(entity.getQuotaBytes()))")
    @Mapping(target = "usedBytes", expression = "java(WireValues.int64(entity.getUsedBytes()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    StorageQuota toDto(StorageQuotaEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "quotaBytes", expression = "java(WireValues.int64(dto.getQuotaBytes()))")
    @Mapping(target = "usedBytes", expression = "java(WireValues.int64(dto.getUsedBytes()))")
    StorageQuotaEntity toEntity(StorageQuota dto);
}
