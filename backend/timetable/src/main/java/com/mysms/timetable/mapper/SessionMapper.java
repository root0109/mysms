package com.mysms.timetable.mapper;

import com.mysms.timetable.api.model.Session;
import com.mysms.core.web.Etags;
import com.mysms.timetable.domain.SessionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps timetable.session to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = TimetableMapperConfig.class, imports = {Etags.class})
public interface SessionMapper {

    @Mapping(target = "status", expression =
            "java(entity.getStatus() == null ? null : Session.StatusEnum.fromValue(entity.getStatus()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Session toDto(SessionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "status", ignore = true)
    SessionEntity toEntity(Session dto);
}
