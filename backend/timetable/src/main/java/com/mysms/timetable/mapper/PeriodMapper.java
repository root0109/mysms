package com.mysms.timetable.mapper;

import com.mysms.timetable.api.model.Period;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.timetable.domain.PeriodEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps timetable.period to and from its contract DTO. The way in ignores id, tenant, audit and read-only fields, so a
 * request cannot set them; the ETag comes from row_version.
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
