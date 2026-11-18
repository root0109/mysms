package com.mysms.platform.mapper;

import com.mysms.platform.api.model.AuditLog;
import com.mysms.platform.repository.AuditLogRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * An audit record → the contract's AuditLog. The stored properties are JSON text the service parses; the trail is
 * immutable, so a record has no version and no ETag.
 */
@Mapper(config = PlatformMapperConfig.class)
public interface AuditLogMapper {

    @Mapping(target = "properties", ignore = true)
    @Mapping(target = "etag", ignore = true)
    AuditLog toDto(AuditLogRow row);
}
