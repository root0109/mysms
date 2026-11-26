package com.mysms.academics.timetable.mapper;

import com.mysms.academics.timetable.api.model.SessionStudent;
import com.mysms.core.web.Etags;
import com.mysms.academics.timetable.domain.SessionStudentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps timetable.session_student to and from its contract DTO. The way in ignores id, tenant, audit and read-only
 * fields, so a request cannot set them; the ETag comes from row_version.
 */
@Mapper(config = TimetableMapperConfig.class, imports = {Etags.class})
public interface SessionStudentMapper {


    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    SessionStudent toDto(SessionStudentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "sessionId", ignore = true)
    SessionStudentEntity toEntity(SessionStudent dto);
}
