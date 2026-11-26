package com.mysms.campus.frontoffice.mapper;

import com.mysms.campus.frontoffice.api.model.VisitorLog;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.campus.frontoffice.domain.VisitorLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps frontoffice.visitor_log to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields,
 * so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = FrontofficeMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface VisitorLogMapper {

    @Mapping(target = "entryTime", expression = "java(WireValues.time(entity.getEntryTime()))")
    @Mapping(target = "exitTime", expression = "java(WireValues.time(entity.getExitTime()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    VisitorLog toDto(VisitorLogEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "entryTime", expression = "java(WireValues.time(dto.getEntryTime()))")
    @Mapping(target = "exitTime", expression = "java(WireValues.time(dto.getExitTime()))")
    VisitorLogEntity toEntity(VisitorLog dto);
}
