package com.mysms.transport.mapper;

import com.mysms.transport.api.model.RouteStop;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.transport.domain.RouteStopEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps transport.route_stop to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so
 * a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = TransportMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface RouteStopMapper {

    @Mapping(target = "pickupTime", expression = "java(WireValues.time(entity.getPickupTime()))")
    @Mapping(target = "dropTime", expression = "java(WireValues.time(entity.getDropTime()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    RouteStop toDto(RouteStopEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "routeId", ignore = true)
    @Mapping(target = "pickupTime", expression = "java(WireValues.time(dto.getPickupTime()))")
    @Mapping(target = "dropTime", expression = "java(WireValues.time(dto.getDropTime()))")
    RouteStopEntity toEntity(RouteStop dto);
}
