package com.mysms.timetable.mapper;

import com.mysms.timetable.api.model.Period;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.timetable.domain.PeriodEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for timetable.period. The way in ignores the id, the tenant, the audit set and every read-only
 * field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = TimetableMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface PeriodMapper {

    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "durationMinutes", expression = "java(WireValues.decimal(entity.getDurationMinutes()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Period toDto(PeriodEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "durationMinutes", expression = "java(WireValues.decimal(dto.getDurationMinutes()))")
    PeriodEntity toEntity(Period dto);
}
