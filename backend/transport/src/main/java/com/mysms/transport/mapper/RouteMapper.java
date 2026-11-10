package com.mysms.transport.mapper;

import com.mysms.transport.api.model.Route;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.transport.domain.RouteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for transport.route. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = TransportMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface RouteMapper {

    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(entity.getEndTime()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Route toDto(RouteEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(dto.getEndTime()))")
    RouteEntity toEntity(Route dto);
}
