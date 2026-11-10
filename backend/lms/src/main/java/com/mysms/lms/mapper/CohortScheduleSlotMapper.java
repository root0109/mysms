package com.mysms.lms.mapper;

import com.mysms.lms.api.model.CohortScheduleSlot;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.web.Etags;
import com.mysms.lms.domain.CohortScheduleSlotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Entity ⇄ contract DTO for lms.cohort_schedule_slot. The way in ignores the id, the tenant, the audit set and every
 * read-only field, so
 * a request can never choose an id, re-tenant a row or set a value only workflows change; the ETag always comes from
 * row_version.
 */
@Mapper(config = LmsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface CohortScheduleSlotMapper {

    @Mapping(target = "slotType", expression =
            "java(entity.getSlotType() == null ? null : "
                    + "CohortScheduleSlot.SlotTypeEnum.fromValue(entity.getSlotType()))")
    @Mapping(target = "weekday", expression =
            "java(entity.getWeekday() == null ? null : CohortScheduleSlot.WeekdayEnum.fromValue(entity.getWeekday()))")
    @Mapping(target = "startTime", expression = "java(WireValues.time(entity.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(entity.getEndTime()))")

    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    CohortScheduleSlot toDto(CohortScheduleSlotEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "cohortId", ignore = true)
    @Mapping(target = "slotType", expression = "java(dto.getSlotType() == null ? null : dto.getSlotType().getValue())")
    @Mapping(target = "weekday", expression = "java(dto.getWeekday() == null ? null : dto.getWeekday().getValue())")
    @Mapping(target = "startTime", expression = "java(WireValues.time(dto.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(WireValues.time(dto.getEndTime()))")
    CohortScheduleSlotEntity toEntity(CohortScheduleSlot dto);
}
